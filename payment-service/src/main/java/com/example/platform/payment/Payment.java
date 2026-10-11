package com.example.platform.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    public enum Status { COMPLETED, FAILED }

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String reason;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected Payment() {
    }

    public Payment(UUID orderId, BigDecimal amount, Status status, String reason) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
