package com.example.platform.inventory;

import com.example.platform.common.Events;
import com.example.platform.common.OutboxService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private final StockItemRepository stock = mock(StockItemRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final InventoryService service = new InventoryService(stock, reservations, outbox);

    private final UUID orderId = UUID.randomUUID();

    @Test
    void reservesStockAndEmitsStockReserved() {
        var item = new StockItem("LAPTOP", 5);
        when(stock.findById("LAPTOP")).thenReturn(Optional.of(item));

        service.reserve(new Events.OrderCreated(orderId, "LAPTOP", 2, new BigDecimal("500")));

        assertThat(item.getAvailable()).isEqualTo(3);
        verify(reservations).save(any(Reservation.class));
        verify(outbox).publish(eq(Events.Topics.INVENTORY), eq(Events.Types.STOCK_RESERVED),
                eq(orderId.toString()), eq(new Events.StockReserved(orderId, new BigDecimal("500"))));
    }

    @Test
    void rejectsWhenStockIsInsufficient() {
        var item = new StockItem("LAPTOP", 1);
        when(stock.findById("LAPTOP")).thenReturn(Optional.of(item));

        service.reserve(new Events.OrderCreated(orderId, "LAPTOP", 2, BigDecimal.TEN));

        assertThat(item.getAvailable()).isEqualTo(1);
        verify(reservations, never()).save(any());
        verify(outbox).publish(eq(Events.Topics.INVENTORY), eq(Events.Types.STOCK_REJECTED),
                eq(orderId.toString()), any(Events.StockRejected.class));
    }

    @Test
    void duplicateOrderCreatedIsIgnored() {
        when(reservations.existsById(orderId)).thenReturn(true);

        service.reserve(new Events.OrderCreated(orderId, "LAPTOP", 1, BigDecimal.TEN));

        verify(outbox, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    void releaseRestoresStockOnlyOnce() {
        var item = new StockItem("PHONE", 4);
        var reservation = new Reservation(orderId, "PHONE", 3);
        when(reservations.findById(orderId)).thenReturn(Optional.of(reservation));
        when(stock.findById("PHONE")).thenReturn(Optional.of(item));

        service.release(orderId);
        service.release(orderId);

        assertThat(item.getAvailable()).isEqualTo(7);
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.RELEASED);
    }
}
