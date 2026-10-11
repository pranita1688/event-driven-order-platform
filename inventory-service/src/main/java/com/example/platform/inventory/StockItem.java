package com.example.platform.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_item")
public class StockItem {

    @Id
    private String sku;

    private int available;

    /** Optimistic locking: two concurrent reservations of the last unit cannot both succeed. */
    @Version
    private long version;

    protected StockItem() {
    }

    public StockItem(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }

    public boolean tryReserve(int quantity) {
        if (available < quantity) {
            return false;
        }
        available -= quantity;
        return true;
    }

    public void release(int quantity) {
        available += quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }
}
