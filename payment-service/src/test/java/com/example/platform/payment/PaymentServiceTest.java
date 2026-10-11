package com.example.platform.payment;

import com.example.platform.common.Events;
import com.example.platform.common.OutboxService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final PaymentService service = new PaymentService(repository, outbox, new BigDecimal("1000"));
    private final UUID orderId = UUID.randomUUID();

    @Test
    void chargesAmountWithinLimit() {
        service.charge(new Events.StockReserved(orderId, new BigDecimal("1000.00")));

        verify(outbox).publish(eq(Events.Topics.PAYMENTS), eq(Events.Types.PAYMENT_COMPLETED),
                eq(orderId.toString()), eq(new Events.PaymentCompleted(orderId)));
    }

    @Test
    void declinesAmountAboveLimit() {
        service.charge(new Events.StockReserved(orderId, new BigDecimal("1000.01")));

        verify(outbox).publish(eq(Events.Topics.PAYMENTS), eq(Events.Types.PAYMENT_FAILED),
                eq(orderId.toString()), any(Events.PaymentFailed.class));
    }

    @Test
    void doesNotChargeTwice() {
        when(repository.existsById(orderId)).thenReturn(true);

        service.charge(new Events.StockReserved(orderId, BigDecimal.TEN));

        verify(outbox, never()).publish(anyString(), anyString(), anyString(), any());
    }
}
