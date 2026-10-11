package com.example.platform.order;

import com.example.platform.common.Events;
import com.example.platform.common.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OutboxService outbox;

    public OrderService(OrderRepository orders, OutboxService outbox) {
        this.orders = orders;
        this.outbox = outbox;
    }

    /** Saves the order and its OrderCreated event in one transaction (transactional outbox). */
    @Transactional
    public Order create(String sku, int quantity, BigDecimal amount) {
        Order order = orders.save(new Order(sku, quantity, amount));
        outbox.publish(Events.Topics.ORDERS, Events.Types.ORDER_CREATED, order.getId().toString(),
                new Events.OrderCreated(order.getId(), sku, quantity, amount));
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<Order> find(UUID id) {
        return orders.findById(id);
    }

    @Transactional
    public void confirm(UUID orderId) {
        transition(orderId, Order.Status.CONFIRMED, null);
    }

    @Transactional
    public void reject(UUID orderId, String reason) {
        transition(orderId, Order.Status.REJECTED, reason);
    }

    @Transactional
    public void cancel(UUID orderId, String reason) {
        transition(orderId, Order.Status.CANCELLED, reason);
    }

    private void transition(UUID orderId, Order.Status target, String reason) {
        orders.findById(orderId).ifPresent(order -> order.transitionTo(target, reason));
    }
}
