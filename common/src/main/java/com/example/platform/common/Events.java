package com.example.platform.common;

import java.math.BigDecimal;
import java.util.UUID;

/** Topic names, event types and payloads shared by all services. */
public final class Events {

    private Events() {
    }

    public static final class Topics {
        public static final String ORDERS = "orders.events";
        public static final String INVENTORY = "inventory.events";
        public static final String PAYMENTS = "payments.events";

        private Topics() {
        }
    }

    public static final class Types {
        public static final String ORDER_CREATED = "OrderCreated";
        public static final String STOCK_RESERVED = "StockReserved";
        public static final String STOCK_REJECTED = "StockRejected";
        public static final String PAYMENT_COMPLETED = "PaymentCompleted";
        public static final String PAYMENT_FAILED = "PaymentFailed";

        private Types() {
        }
    }

    public record OrderCreated(UUID orderId, String sku, int quantity, BigDecimal amount) {
    }

    public record StockReserved(UUID orderId, BigDecimal amount) {
    }

    public record StockRejected(UUID orderId, String reason) {
    }

    public record PaymentCompleted(UUID orderId) {
    }

    public record PaymentFailed(UUID orderId, String reason) {
    }
}
