package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStareChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Boolean.TRUE;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";
    private final StateMachineFactory<BeerOrderState, BeerOrderEvent> stateMachineFactory;
    private final BeerOrderStareChangeInterceptor beerOrderStareChangeInterceptor;
    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderState.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        log.debug("Process validation result for beerOrderId: {}. Valid? {}", beerOrderId, isValid);
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            if (TRUE.equals(isValid)) {
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_PASSED);
                awaitForStatus(beerOrderId, BeerOrderState.VALIDATED);
                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).orElseThrow();
                sendBeerOrderEvent(validatedOrder, BeerOrderEvent.ALLOCATE_ORDER);
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_FAILED);
            }
        }, () -> log.error("Order not found. Id: {}", beerOrderId));

    }

    @Transactional
    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_SUCCESS);
            awaitForStatus(beerOrder.getId(), BeerOrderState.ALLOCATED);
            updateAllocatedQty(beerOrderDto);
        }, () -> log.error("Order not found. Id: {}", beerOrderDto.getId()));
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                });
            });
            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("Order not found. Id: {}", beerOrderDto.getId()));
    }

    @Transactional
    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_NO_INVENTORY);
            awaitForStatus(beerOrder.getId(), BeerOrderState.PENDING_INVENTORY);
            updateAllocatedQty(beerOrderDto);
        }, () -> log.error("Order not found. Id: {}", beerOrderDto.getId()));
    }

    @Transactional
    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder ->
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_FAILED), () ->
                log.error("Order not found. Id: {}", beerOrderDto.getId()));
    }

    @Override
    public void beerOrderPickedUp(UUID id) {
        beerOrderRepository.findById(id).ifPresentOrElse(beerOrder ->
                        sendBeerOrderEvent(beerOrder, BeerOrderEvent.BEER_ORDER_PICKED_UP),
                () -> log.error("Order not found. Id: {}", id));
    }

    @Override
    public void cancelOrder(UUID id) {
        beerOrderRepository.findById(id).ifPresentOrElse(beerOrder ->
                        sendBeerOrderEvent(beerOrder, BeerOrderEvent.CANCEL_ORDER),
                () -> log.error("Order not found. Id: {}", id));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEvent event) {
        StateMachine<BeerOrderState, BeerOrderEvent> stateMachine = build(beerOrder);
        Message<BeerOrderEvent> message = MessageBuilder.withPayload(event)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();
        stateMachine.sendEvent(message);
    }

    @SneakyThrows
    private void awaitForStatus(UUID beerOrderId, BeerOrderState state) {
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (state.equals(beerOrder.getOrderStatus())) {
                    found.set(true);
                    log.debug("Order found");
                } else {
                    log.debug("Order status not equal. Expected: {}. Found: {}",
                            state, beerOrder.getOrderStatus());
                }
            }, () -> log.debug("Order id not found"));

            if (!found.get()) {
                log.debug("Sleeping for retry");
                Thread.sleep(100);
            }

        }

    }

    private StateMachine<BeerOrderState, BeerOrderEvent> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderState, BeerOrderEvent> stateMachine = stateMachineFactory
                .getStateMachine(beerOrder.getId());

        stateMachine.stop();
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(beerOrderStareChangeInterceptor);
                    sma.resetStateMachine(
                            new DefaultStateMachineContext<>(beerOrder.getOrderStatus(),
                                    null, null, null)
                    );
                });

        stateMachine.start();

        return stateMachine;
    }
}
