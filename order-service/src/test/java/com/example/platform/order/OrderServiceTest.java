package com.example.platform.order;

import com.example.platform.common.Events;
import com.example.platform.common.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final OrderService service = new OrderService(repository, outbox);

    @Test
    void createSavesPendingOrderAndPublishesOrderCreated() {
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = service.create("LAPTOP", 2, new BigDecimal("999.00"));

        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq(Events.Topics.ORDERS), eq(Events.Types.ORDER_CREATED),
                eq(order.getId().toString()), payload.capture());
        assertThat(payload.getValue()).isEqualTo(
                new Events.OrderCreated(order.getId(), "LAPTOP", 2, new BigDecimal("999.00")));
    }

    @Test
    void settledOrderIgnoresLaterOutcomes() {
        Order order = new Order("PHONE", 1, BigDecimal.TEN);
        when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        service.confirm(order.getId());
        service.cancel(order.getId(), "late failure event");

        assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);
        assertThat(order.getFailureReason()).isNull();
    }
}
