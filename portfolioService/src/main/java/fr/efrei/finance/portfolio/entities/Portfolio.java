package fr.efrei.finance.portfolio.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A portfolio: cash + stock positions owned by a client.
 */
@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 60)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cashBalance;

    @Column(nullable = false)
    private Instant createdAt;

    /** Optimistic locking: two concurrent trades on the same portfolio cannot both win. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("symbol ASC")
    private List<Position> positions = new ArrayList<>();

    protected Portfolio() {
        // required by JPA
    }

    public Portfolio(String name, String owner, BigDecimal initialCash) {
        this.name = name;
        this.owner = owner;
        this.cashBalance = initialCash;
        this.createdAt = Instant.now();
    }

    public Optional<Position> findPosition(String symbol) {
        return positions.stream().filter(p -> p.getSymbol().equals(symbol)).findFirst();
    }

    public Position addPosition(String symbol) {
        Position position = new Position(this, symbol);
        positions.add(position);
        return position;
    }

    public void removePosition(Position position) {
        positions.remove(position);
    }

    public void credit(BigDecimal amount) {
        cashBalance = cashBalance.add(amount);
    }

    public void debit(BigDecimal amount) {
        cashBalance = cashBalance.subtract(amount);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Position> getPositions() { return positions; }
}
