package com.example.platform.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock")
public class StockController {

    public record StockView(String sku, int available) {
    }

    private final StockItemRepository stock;

    public StockController(StockItemRepository stock) {
        this.stock = stock;
    }

    @GetMapping
    public List<StockView> all() {
        return stock.findAll().stream().map(s -> new StockView(s.getSku(), s.getAvailable())).toList();
    }
}
