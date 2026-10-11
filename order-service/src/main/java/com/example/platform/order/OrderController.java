package com.example.platform.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    public record CreateOrderRequest(
            @NotBlank String sku,
            @Min(1) int quantity,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    public record OrderResponse(UUID id, String sku, int quantity, BigDecimal amount, String status,
                                String failureReason) {
        static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getSku(), o.getQuantity(), o.getAmount(),
                    o.getStatus().name(), o.getFailureReason());
        }
    }

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** 202 because the order is accepted now and settled asynchronously by the saga. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orders.create(request.sku(), request.quantity(), request.amount());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return orders.find(id).map(OrderResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
