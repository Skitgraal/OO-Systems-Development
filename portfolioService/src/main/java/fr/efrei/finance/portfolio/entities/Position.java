package fr.efrei.finance.portfolio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A holding of one instrument inside a portfolio, with its weighted average cost.
 */
@Entity
@Table(name = "portfolio_position",uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "symbol"}))
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost = BigDecimal.ZERO;

    protected Position() {
        // required by JPA
    }

    Position(Portfolio portfolio, String symbol) {
        this.portfolio = portfolio;
        this.symbol = symbol;
    }

    /** Adds shares bought at a given price and updates the weighted average cost. */
    public void addShares(int boughtQuantity, BigDecimal price) {
        BigDecimal oldCost = averageCost.multiply(BigDecimal.valueOf(quantity));
        BigDecimal newCost = price.multiply(BigDecimal.valueOf(boughtQuantity));
        quantity += boughtQuantity;
        averageCost = oldCost.add(newCost).divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP);
    }

    /** Removes sold shares; the average cost does not change on a sale. */
    public void removeShares(int soldQuantity) {
        quantity -= soldQuantity;
    }

    public BigDecimal getCostBasis() {
        return averageCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Portfolio getPortfolio() { return portfolio; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public BigDecimal getAverageCost() { return averageCost; }
}
