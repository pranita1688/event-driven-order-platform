package com.example.platform.payment;

import com.example.platform.common.Events;
import com.example.platform.common.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Mock payment provider: declines anything above a configurable limit. */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final OutboxService outbox;
    private final BigDecimal maxAmount;

    public PaymentService(PaymentRepository payments, OutboxService outbox,
                          @Value("${payment.max-amount:1000}") BigDecimal maxAmount) {
        this.payments = payments;
        this.outbox = outbox;
        this.maxAmount = maxAmount;
    }

    @Transactional
    public void charge(Events.StockReserved reserved) {
        if (payments.existsById(reserved.orderId())) {
            return;
        }
        String orderId = reserved.orderId().toString();
        if (reserved.amount().compareTo(maxAmount) > 0) {
            String reason = "Amount exceeds limit of " + maxAmount;
            payments.save(new Payment(reserved.orderId(), reserved.amount(), Payment.Status.FAILED, reason));
            outbox.publish(Events.Topics.PAYMENTS, Events.Types.PAYMENT_FAILED, orderId,
                    new Events.PaymentFailed(reserved.orderId(), reason));
        } else {
            payments.save(new Payment(reserved.orderId(), reserved.amount(), Payment.Status.COMPLETED, null));
            outbox.publish(Events.Topics.PAYMENTS, Events.Types.PAYMENT_COMPLETED, orderId,
                    new Events.PaymentCompleted(reserved.orderId()));
        }
    }
}
