package fr.efrei.finance.portfolio.services;

import fr.efrei.finance.portfolio.client.QuoteClient;
import fr.efrei.finance.portfolio.dto.PortfolioDto;
import fr.efrei.finance.portfolio.dto.PortfolioSummaryDto;
import fr.efrei.finance.portfolio.dto.PositionDto;
import fr.efrei.finance.portfolio.dto.QuoteDto;
import fr.efrei.finance.portfolio.dto.TransactionDto;
import fr.efrei.finance.portfolio.entities.Portfolio;
import fr.efrei.finance.portfolio.entities.Position;
import fr.efrei.finance.portfolio.entities.Transaction;
import fr.efrei.finance.portfolio.entities.TransactionType;
import fr.efrei.finance.portfolio.exceptions.InsufficientFundsException;
import fr.efrei.finance.portfolio.exceptions.InsufficientQuantityException;
import fr.efrei.finance.portfolio.exceptions.ResourceNotFoundException;
import fr.efrei.finance.portfolio.repositories.PortfolioRepository;
import fr.efrei.finance.portfolio.repositories.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Business layer: portfolio management, trading and valuation.
 * Prices always come from the quote server (gRPC) at the time of the operation.
 */
@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final QuoteClient quoteClient;

    @Autowired
    public PortfolioService(PortfolioRepository portfolioRepository,
                            TransactionRepository transactionRepository,
                            QuoteClient quoteClient) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.quoteClient = quoteClient;
    }

    // ---------------------------------------------------------------- portfolios

    @Transactional
    public PortfolioSummaryDto createPortfolio(String name, String owner, BigDecimal initialCash) {
        BigDecimal cash = initialCash == null ? BigDecimal.ZERO : money(initialCash);
        Portfolio portfolio = portfolioRepository.save(new Portfolio(name.trim(), owner.trim(), cash));
        if (cash.signum() > 0) {
            transactionRepository.save(Transaction.cash(portfolio.getId(), TransactionType.DEPOSIT, cash));
        }
        log.info("Portfolio {} created for owner '{}' with {} EUR", portfolio.getId(), owner, cash);
        return toSummary(portfolio);
    }

    @Transactional(readOnly = true)
    public List<PortfolioSummaryDto> listPortfolios() {
        return portfolioRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PortfolioService::toSummary)
                .toList();
    }

    @Transactional
    public void deletePortfolio(Long id) {
        Portfolio portfolio = findPortfolio(id);
        transactionRepository.deleteByPortfolioId(id);
        portfolioRepository.delete(portfolio);
        log.info("Portfolio {} deleted", id);
    }

    /** Values every position at the current market price. */
    @Transactional(readOnly = true)
    public PortfolioDto getValuation(Long id) {
        Portfolio portfolio = findPortfolio(id);
        List<String> symbols = portfolio.getPositions().stream().map(Position::getSymbol).toList();
        Map<String, QuoteDto> quotes = quoteClient.getQuotes(symbols);

        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal costBasis = BigDecimal.ZERO;
        List<PositionDto> lines = new ArrayList<>();
        for (Position position : portfolio.getPositions()) {
            QuoteDto quote = quotes.get(position.getSymbol());
            // If a quote is missing, fall back to the cost price so the valuation still works.
            BigDecimal price = quote != null ? quote.price() : position.getAverageCost();
            BigDecimal marketValue = money(price.multiply(BigDecimal.valueOf(position.getQuantity())));
            BigDecimal cost = position.getCostBasis();
            BigDecimal pnl = marketValue.subtract(cost);
            invested = invested.add(marketValue);
            costBasis = costBasis.add(cost);
            lines.add(new PositionDto(
                    position.getSymbol(),
                    quote != null ? quote.name() : position.getSymbol(),
                    position.getQuantity(),
                    position.getAverageCost().setScale(2, RoundingMode.HALF_UP),
                    price,
                    quote != null ? quote.changePercent() : BigDecimal.ZERO,
                    marketValue,
                    pnl,
                    percent(pnl, cost),
                    BigDecimal.ZERO)); // weight computed below, once the total is known
        }

        BigDecimal total = portfolio.getCashBalance().add(invested);
        List<PositionDto> weighted = lines.stream()
                .map(p -> new PositionDto(p.symbol(), p.name(), p.quantity(), p.averageCost(), p.marketPrice(),
                        p.dayChangePercent(), p.marketValue(), p.unrealizedPnl(), p.unrealizedPnlPercent(),
                        percent(p.marketValue(), total)))
                .toList();

        BigDecimal unrealizedPnl = invested.subtract(costBasis);
        return new PortfolioDto(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getOwner(),
                portfolio.getCreatedAt(),
                portfolio.getCashBalance(),
                invested,
                total,
                costBasis,
                unrealizedPnl,
                percent(unrealizedPnl, costBasis),
                Instant.now(),
                weighted);
    }

    // ---------------------------------------------------------------- cash

    @Transactional
    public PortfolioSummaryDto deposit(Long id, BigDecimal amount) {
        Portfolio portfolio = findPortfolio(id);
        BigDecimal value = money(amount);
        portfolio.credit(value);
        transactionRepository.save(Transaction.cash(id, TransactionType.DEPOSIT, value));
        log.info("Portfolio {}: deposit of {} EUR", id, value);
        return toSummary(portfolio);
    }

    @Transactional
    public PortfolioSummaryDto withdraw(Long id, BigDecimal amount) {
        Portfolio portfolio = findPortfolio(id);
        BigDecimal value = money(amount);
        if (portfolio.getCashBalance().compareTo(value) < 0) {
            throw new InsufficientFundsException(value, portfolio.getCashBalance());
        }
        portfolio.debit(value);
        transactionRepository.save(Transaction.cash(id, TransactionType.WITHDRAWAL, value.negate()));
        log.info("Portfolio {}: withdrawal of {} EUR", id, value);
        return toSummary(portfolio);
    }

    // ---------------------------------------------------------------- trading

    /** Buys shares at the current market price (market order). */
    @Transactional
    public TransactionDto buy(Long id, String symbol, int quantity) {
        Portfolio portfolio = findPortfolio(id);
        QuoteDto quote = quoteClient.getQuote(symbol);
        BigDecimal cost = money(quote.price().multiply(BigDecimal.valueOf(quantity)));

        if (portfolio.getCashBalance().compareTo(cost) < 0) {
            log.warn("Portfolio {}: buy {} x {} rejected, insufficient funds", id, quantity, quote.symbol());
            throw new InsufficientFundsException(cost, portfolio.getCashBalance());
        }

        portfolio.debit(cost);
        Position position = portfolio.findPosition(quote.symbol())
                .orElseGet(() -> portfolio.addPosition(quote.symbol()));
        position.addShares(quantity, quote.price());

        Transaction tx = transactionRepository.save(
                Transaction.buy(id, quote.symbol(), quantity, quote.price(), cost));
        log.info("Portfolio {}: bought {} x {} at {} EUR (total {} EUR)",
                id, quantity, quote.symbol(), quote.price(), cost);
        return toDto(tx);
    }

    /** Sells shares at the current market price (market order) and books the realized P&L. */
    @Transactional
    public TransactionDto sell(Long id, String symbol, int quantity) {
        Portfolio portfolio = findPortfolio(id);
        QuoteDto quote = quoteClient.getQuote(symbol);
        Position position = portfolio.findPosition(quote.symbol())
                .orElseThrow(() -> new InsufficientQuantityException(quote.symbol(), quantity, 0));
        if (position.getQuantity() < quantity) {
            throw new InsufficientQuantityException(quote.symbol(), quantity, position.getQuantity());
        }

        BigDecimal proceeds = money(quote.price().multiply(BigDecimal.valueOf(quantity)));
        BigDecimal costOfSold = money(position.getAverageCost().multiply(BigDecimal.valueOf(quantity)));
        BigDecimal realizedPnl = proceeds.subtract(costOfSold);

        position.removeShares(quantity);
        if (position.getQuantity() == 0) {
            portfolio.removePosition(position);
        }
        portfolio.credit(proceeds);

        Transaction tx = transactionRepository.save(
                Transaction.sell(id, quote.symbol(), quantity, quote.price(), proceeds, realizedPnl));
        log.info("Portfolio {}: sold {} x {} at {} EUR (realized P&L {} EUR)",
                id, quantity, quote.symbol(), quote.price(), realizedPnl);
        return toDto(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactions(Long id) {
        findPortfolio(id); // 404 if the portfolio does not exist
        return transactionRepository.findByPortfolioIdOrderByExecutedAtDesc(id).stream()
                .map(PortfolioService::toDto)
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    private Portfolio findPortfolio(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio " + id + " not found"));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private static PortfolioSummaryDto toSummary(Portfolio p) {
        return new PortfolioSummaryDto(p.getId(), p.getName(), p.getOwner(), p.getCashBalance(),
                p.getPositions().size(), p.getCreatedAt());
    }

    private static TransactionDto toDto(Transaction t) {
        return new TransactionDto(t.getId(), t.getType().name(), t.getSymbol(), t.getQuantity(),
                t.getPrice() == null ? null : t.getPrice().setScale(2, RoundingMode.HALF_UP),
                t.getAmount(), t.getRealizedPnl(), t.getExecutedAt());
    }
}
