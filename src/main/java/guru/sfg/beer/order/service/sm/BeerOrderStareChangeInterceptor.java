package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEvent;
import guru.sfg.beer.order.service.domain.BeerOrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStareChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderState, BeerOrderEvent> {

    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @Override
    public void preStateChange(State<BeerOrderState, BeerOrderEvent> state, Message<BeerOrderEvent> message,
                               Transition<BeerOrderState, BeerOrderEvent> transition,
                               StateMachine<BeerOrderState, BeerOrderEvent> stateMachine) {
        Optional.ofNullable(message)
                .map(msg -> (String) msg.getHeaders()
                        .getOrDefault(BeerOrderManagerImpl.ORDER_ID_HEADER, " "))
                .ifPresent(orderId -> {
                    log.debug("Saving state for order id: " + orderId + " | Status: " + state.getId());

                    BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(orderId));
                    beerOrder.setOrderStatus(state.getId());
                    beerOrderRepository.saveAndFlush(beerOrder);
                });
    }
}
