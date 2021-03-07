package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStareChangeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.lang.Boolean.TRUE;

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
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);

        if (TRUE.equals(isValid)) {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_PASSED);

            BeerOrder validatedOrder = beerOrderRepository.findOneById(beerOrderId);
            sendBeerOrderEvent(validatedOrder, BeerOrderEvent.ALLOCATE_ORDER);
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_FAILED);
        }
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEvent event) {
        StateMachine<BeerOrderState, BeerOrderEvent> stateMachine = build(beerOrder);
        Message<BeerOrderEvent> message = MessageBuilder.withPayload(event)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();
        stateMachine.sendEvent(message);
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
