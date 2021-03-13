package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidationFailureAction implements Action<BeerOrderState, BeerOrderEvent> {

    @Override
    public void execute(StateContext<BeerOrderState, BeerOrderEvent> context) {
        String strBeerOrderId = (String) context.getMessage()
                .getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);
        log.error("Compensating transaction...... Validation failed: {}", strBeerOrderId);
    }
}
