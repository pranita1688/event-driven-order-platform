package com.example.platform.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "reservation")
public class Reservation {

    public enum Status { RESERVED, RELEASED }

    @Id
    @jakarta.persistence.Column(name = "order_id")
    private UUID orderId;

    private String sku;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private Status status;

    protected Reservation() {
    }

    public Reservation(UUID orderId, String sku, int quantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = Status.RESERVED;
    }

    public boolean release() {
        if (status != Status.RESERVED) {
            return false;
        }
        status = Status.RELEASED;
        return true;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public Status getStatus() {
        return status;
    }
}
