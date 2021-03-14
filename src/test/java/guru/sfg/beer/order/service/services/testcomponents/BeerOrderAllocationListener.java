package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderLineDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderAllocationListener {

    public static final String FAIL_ALLOCATION = "fail-allocation";
    public static final String PARTIAL_ALLOCATION = "partial-allocation";
    public static final String DONT_ALLOCATE = "dont-allocate";
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message message) {
        AllocateOrderRequest request = (AllocateOrderRequest) message.getPayload();

        boolean pendingInventory = false;
        boolean allocationError = false;
        boolean sendResponse = true;

        if (FAIL_ALLOCATION.equals(request.getBeerOrderDto().getCustomerRef())) {
            allocationError = true;
        } else if (PARTIAL_ALLOCATION.equals(request.getBeerOrderDto().getCustomerRef())) {
            pendingInventory = true;
        } else if (DONT_ALLOCATE.equals(request.getBeerOrderDto().getCustomerRef())) {
            sendResponse = false;
        }

        for (BeerOrderLineDto beerOrderLineDto : request.getBeerOrderDto().getBeerOrderLines()) {
            if (pendingInventory) {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
            } else {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        }

        if (sendResponse) {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                    AllocateOrderResult.builder()
                            .beerOrderDto(request.getBeerOrderDto())
                            .pendingInventory(pendingInventory)
                            .allocationError(allocationError)
                            .build());
        }
    }

}
