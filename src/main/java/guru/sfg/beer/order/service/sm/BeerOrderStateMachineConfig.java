package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderState, BeerOrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderState, BeerOrderEvent> states) throws Exception {
        states.withStates()
                .initial(BeerOrderState.NEW)
                .states(EnumSet.allOf(BeerOrderState.class))
                .end(BeerOrderState.PICKED_UP)
                .end(BeerOrderState.DELIVERED)
                .end(BeerOrderState.DELIVERY_EXCEPTION)
                .end(BeerOrderState.VALIDATION_EXCEPTION)
                .end(BeerOrderState.ALLOCATION_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderState, BeerOrderEvent> transitions) throws Exception {
        transitions.withExternal()
                .source(BeerOrderState.NEW).target(BeerOrderState.NEW)
                .event(BeerOrderEvent.VALIDATE_ORDER)
                .and().withExternal()
                .source(BeerOrderState.NEW).target(BeerOrderState.VALIDATED)
                .event(BeerOrderEvent.VALIDATION_PASSED)
                .and().withExternal()
                .source(BeerOrderState.NEW).target(BeerOrderState.VALIDATION_EXCEPTION)
                .event(BeerOrderEvent.VALIDATION_FAILED);
    }
}
