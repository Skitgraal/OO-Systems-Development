package fr.efrei.finance.portfolio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable record of every cash movement or trade (audit trail).
 */
@Entity(name = "PortfolioTransaction")
@Table(name = "portfolio_transaction", indexes = @Index(columnList = "portfolio_id"))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Column(length = 16)
    private String symbol;

    private Integer quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    /** Cash impact: positive = cash in, negative = cash out. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Realized profit or loss (sales only). */
    @Column(precision = 19, scale = 2)
    private BigDecimal realizedPnl;

    @Column(nullable = false)
    private Instant executedAt;

    protected Transaction() {
        // required by JPA
    }

    private Transaction(Long portfolioId, TransactionType type, String symbol, Integer quantity,
                        BigDecimal price, BigDecimal amount, BigDecimal realizedPnl) {
        this.portfolioId = portfolioId;
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.realizedPnl = realizedPnl;
        this.executedAt = Instant.now();
    }

    public static Transaction cash(Long portfolioId, TransactionType type, BigDecimal signedAmount) {
        return new Transaction(portfolioId, type, null, null, null, signedAmount, null);
    }

    public static Transaction buy(Long portfolioId, String symbol, int quantity, BigDecimal price, BigDecimal cost) {
        return new Transaction(portfolioId, TransactionType.BUY, symbol, quantity, price, cost.negate(), null);
    }

    public static Transaction sell(Long portfolioId, String symbol, int quantity, BigDecimal price,
                                   BigDecimal proceeds, BigDecimal realizedPnl) {
        return new Transaction(portfolioId, TransactionType.SELL, symbol, quantity, price, proceeds, realizedPnl);
    }

    public Long getId() { return id; }
    public Long getPortfolioId() { return portfolioId; }
    public TransactionType getType() { return type; }
    public String getSymbol() { return symbol; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public Instant getExecutedAt() { return executedAt; }
}
