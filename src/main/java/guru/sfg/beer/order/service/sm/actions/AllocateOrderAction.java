package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocateOrderAction implements Action<BeerOrderState, BeerOrderEvent> {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderState, BeerOrderEvent> context) {
        String strBeerOrderId = (String) context.getMessage()
                .getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);
        if (strBeerOrderId == null) {
            log.warn("Beer order id is null");
        } else {
            beerOrderRepository.findById(UUID.fromString(strBeerOrderId)).ifPresentOrElse(beerOrder -> {
                jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_QUEUE,
                        AllocateOrderRequest.builder()
                                .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                                .build());
                log.debug("Sent Allocation Request for order id: " + strBeerOrderId);
            }, () -> log.error("Order not found. Id: {}", strBeerOrderId));
        }
    }
}
