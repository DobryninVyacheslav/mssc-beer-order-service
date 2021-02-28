package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidateOrderAction implements Action<BeerOrderState, BeerOrderEvent> {

    @Override
    public void execute(StateContext<BeerOrderState, BeerOrderEvent> context) {

    }
}
