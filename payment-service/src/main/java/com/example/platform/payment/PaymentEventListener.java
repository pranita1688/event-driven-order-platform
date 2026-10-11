package com.example.platform.payment;

import com.example.platform.common.Envelope;
import com.example.platform.common.EventCodec;
import com.example.platform.common.Events;
import com.example.platform.common.IdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventListener {

    private final EventCodec codec;
    private final IdempotencyService idempotency;
    private final PaymentService payments;

    public PaymentEventListener(EventCodec codec, IdempotencyService idempotency, PaymentService payments) {
        this.codec = codec;
        this.idempotency = idempotency;
        this.payments = payments;
    }

    @KafkaListener(topics = Events.Topics.INVENTORY, groupId = "payment-service")
    @Transactional
    public void on(String message) {
        Envelope event = codec.parse(message);
        if (Events.Types.STOCK_RESERVED.equals(event.type()) && idempotency.firstTime(event.eventId())) {
            payments.charge(codec.payload(event, Events.StockReserved.class));
        }
    }
}
