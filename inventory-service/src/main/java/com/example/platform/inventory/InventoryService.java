package com.example.platform.inventory;

import com.example.platform.common.Events;
import com.example.platform.common.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    private final OutboxService outbox;

    public InventoryService(StockItemRepository stock, ReservationRepository reservations, OutboxService outbox) {
        this.stock = stock;
        this.reservations = reservations;
        this.outbox = outbox;
    }

    /** Saga step: reserve stock, then tell the world whether it worked. */
    @Transactional
    public void reserve(Events.OrderCreated order) {
        if (reservations.existsById(order.orderId())) {
            return;
        }
        var item = stock.findById(order.sku()).orElse(null);
        if (item == null) {
            reject(order, "Unknown SKU " + order.sku());
        } else if (!item.tryReserve(order.quantity())) {
            reject(order, "Insufficient stock for " + order.sku());
        } else {
            stock.save(item);
            reservations.save(new Reservation(order.orderId(), order.sku(), order.quantity()));
            outbox.publish(Events.Topics.INVENTORY, Events.Types.STOCK_RESERVED, order.orderId().toString(),
                    new Events.StockReserved(order.orderId(), order.amount()));
        }
    }

    /** Compensation: payment failed, so put the stock back. */
    @Transactional
    public void release(UUID orderId) {
        reservations.findById(orderId).ifPresent(reservation -> {
            if (reservation.release()) {
                stock.findById(reservation.getSku()).ifPresent(item -> {
                    item.release(reservation.getQuantity());
                    stock.save(item);
                });
            }
        });
    }

    private void reject(Events.OrderCreated order, String reason) {
        outbox.publish(Events.Topics.INVENTORY, Events.Types.STOCK_REJECTED, order.orderId().toString(),
                new Events.StockRejected(order.orderId(), reason));
    }
}
