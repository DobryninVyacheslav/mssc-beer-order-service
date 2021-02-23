package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;

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
}
