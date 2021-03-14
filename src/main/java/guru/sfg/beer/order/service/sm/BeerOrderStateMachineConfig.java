package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@RequiredArgsConstructor
@EnableStateMachineFactory
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderState, BeerOrderEvent> {

    private final Action<BeerOrderState, BeerOrderEvent> validationFailureAction;
    private final Action<BeerOrderState, BeerOrderEvent> allocationFailureAction;
    private final Action<BeerOrderState, BeerOrderEvent> deallocateOrderAction;
    private final Action<BeerOrderState, BeerOrderEvent> validateOrderAction;
    private final Action<BeerOrderState, BeerOrderEvent> allocateOrderAction;

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderState, BeerOrderEvent> states) throws Exception {
        states.withStates()
                .initial(BeerOrderState.NEW)
                .states(EnumSet.allOf(BeerOrderState.class))
                .end(BeerOrderState.PICKED_UP)
                .end(BeerOrderState.DELIVERED)
                .end(BeerOrderState.CANCELLED)
                .end(BeerOrderState.DELIVERY_EXCEPTION)
                .end(BeerOrderState.VALIDATION_EXCEPTION)
                .end(BeerOrderState.ALLOCATION_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderState, BeerOrderEvent> transitions) throws Exception {
        transitions.withExternal()
                .source(BeerOrderState.NEW).target(BeerOrderState.VALIDATION_PENDING)
                .event(BeerOrderEvent.VALIDATE_ORDER)
                .action(validateOrderAction)
                .and().withExternal()
                .source(BeerOrderState.VALIDATION_PENDING).target(BeerOrderState.VALIDATED)
                .event(BeerOrderEvent.VALIDATION_PASSED)
                .and().withExternal()
                .source(BeerOrderState.VALIDATION_PENDING).target(BeerOrderState.CANCELLED)
                .event(BeerOrderEvent.CANCEL_ORDER)
                .and().withExternal()
                .source(BeerOrderState.VALIDATION_PENDING).target(BeerOrderState.VALIDATION_EXCEPTION)
                .event(BeerOrderEvent.VALIDATION_FAILED)
                .action(validationFailureAction)
                .and().withExternal()
                .source(BeerOrderState.VALIDATED).target(BeerOrderState.ALLOCATION_PENDING)
                .event(BeerOrderEvent.ALLOCATE_ORDER)
                .action(allocateOrderAction)
                .and().withExternal()
                .source(BeerOrderState.VALIDATED).target(BeerOrderState.CANCELLED)
                .event(BeerOrderEvent.CANCEL_ORDER)
                .and().withExternal()
                .source(BeerOrderState.ALLOCATION_PENDING).target(BeerOrderState.ALLOCATED)
                .event(BeerOrderEvent.ALLOCATION_SUCCESS)
                .and().withExternal()
                .source(BeerOrderState.ALLOCATION_PENDING).target(BeerOrderState.ALLOCATION_EXCEPTION)
                .event(BeerOrderEvent.ALLOCATION_FAILED)
                .action(allocationFailureAction)
                .and().withExternal()
                .source(BeerOrderState.ALLOCATION_PENDING).target(BeerOrderState.CANCELLED)
                .event(BeerOrderEvent.CANCEL_ORDER)
                .and().withExternal()
                .source(BeerOrderState.ALLOCATION_PENDING).target(BeerOrderState.PENDING_INVENTORY)
                .event(BeerOrderEvent.ALLOCATION_NO_INVENTORY)
                .and().withExternal()
                .source(BeerOrderState.ALLOCATED).target(BeerOrderState.PICKED_UP)
                .event(BeerOrderEvent.BEER_ORDER_PICKED_UP)
                .and().withExternal()
                .source(BeerOrderState.ALLOCATED).target(BeerOrderState.CANCELLED)
                .event(BeerOrderEvent.CANCEL_ORDER)
                .action(deallocateOrderAction);
    }
}
