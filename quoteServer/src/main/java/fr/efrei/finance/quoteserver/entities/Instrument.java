package fr.efrei.finance.quoteserver.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A tradable instrument (stock) and its last known price.
 */
@Entity
@Table(name = "instrument")
public class Instrument {

    @Id
    @Column(length = 16)
    private String symbol;

    @Column(nullable = false)
    private String name;

    private String sector;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private double lastPrice;

    @Column(nullable = false)
    private double previousClose;

    private Instant updatedAt;

    protected Instrument() {
        // required by JPA
    }

    public Instrument(String symbol, String name, String sector, String currency, double price) {
        this.symbol = symbol;
        this.name = name;
        this.sector = sector;
        this.currency = currency;
        this.lastPrice = price;
        this.previousClose = price;
        this.updatedAt = Instant.now();
    }

    public double getChangePercent() {
        if (previousClose == 0) {
            return 0;
        }
        return (lastPrice - previousClose) / previousClose * 100.0;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public String getCurrency() { return currency; }
    public double getLastPrice() { return lastPrice; }
    public double getPreviousClose() { return previousClose; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
        this.updatedAt = Instant.now();
    }
}
