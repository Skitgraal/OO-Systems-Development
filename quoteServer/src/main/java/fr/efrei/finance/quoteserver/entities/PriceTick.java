package fr.efrei.finance.quoteserver.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Price history: one row each time the price of an instrument moves.
 */
@Entity
@Table(name = "price_tick", indexes = @Index(columnList = "symbol"))
public class PriceTick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private Instant timestamp;

    protected PriceTick() {
        // required by JPA
    }

    public PriceTick(String symbol, double price, Instant timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public Instant getTimestamp() { return timestamp; }
}
