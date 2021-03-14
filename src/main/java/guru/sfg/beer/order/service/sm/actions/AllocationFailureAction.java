package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
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
public class AllocationFailureAction implements Action<BeerOrderState, BeerOrderEvent> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderState, BeerOrderEvent> context) {
        String beerOrderId = (String) context.getMessage()
                .getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);

        if (beerOrderId == null) {
            log.warn("Beer order id is null");
        } else {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_FAILURE_QUEUE, AllocationFailureEvent.builder()
                    .orderId(UUID.fromString(beerOrderId))
                    .build());

            log.debug("Sent allocation failure message to queue for order id: " + beerOrderId);
        }
    }
}
