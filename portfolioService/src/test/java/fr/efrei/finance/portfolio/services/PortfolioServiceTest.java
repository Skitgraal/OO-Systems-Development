package fr.efrei.finance.portfolio.services;

import fr.efrei.finance.portfolio.client.QuoteClient;
import fr.efrei.finance.portfolio.dto.PortfolioDto;
import fr.efrei.finance.portfolio.dto.QuoteDto;
import fr.efrei.finance.portfolio.dto.TransactionDto;
import fr.efrei.finance.portfolio.entities.Portfolio;
import fr.efrei.finance.portfolio.entities.Transaction;
import fr.efrei.finance.portfolio.exceptions.InsufficientFundsException;
import fr.efrei.finance.portfolio.exceptions.InsufficientQuantityException;
import fr.efrei.finance.portfolio.exceptions.ResourceNotFoundException;
import fr.efrei.finance.portfolio.repositories.PortfolioRepository;
import fr.efrei.finance.portfolio.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioServiceTest {

    private PortfolioRepository portfolioRepository;
    private TransactionRepository transactionRepository;
    private QuoteClient quoteClient;
    private PortfolioService service;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolioRepository = mock(PortfolioRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        quoteClient = mock(QuoteClient.class);
        service = new PortfolioService(portfolioRepository, transactionRepository, quoteClient);

        portfolio = new Portfolio("Main", "Sacha", new BigDecimal("1000.00"));
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenPrice(String symbol, String price) {
        when(quoteClient.getQuote(symbol)).thenReturn(quote(symbol, price));
    }

    private static QuoteDto quote(String symbol, String price) {
        BigDecimal p = new BigDecimal(price);
        return new QuoteDto(symbol, symbol, "EUR", p, p, BigDecimal.ZERO, Instant.now());
    }

    @Test
    void buyDebitsCashAndCreatesPosition() {
        givenPrice("BNP.PA", "60.00");

        TransactionDto tx = service.buy(1L, "BNP.PA", 10);

        assertThat(tx.amount()).isEqualByComparingTo("-600.00");
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo("400.00");
        assertThat(portfolio.findPosition("BNP.PA").orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    void secondBuyUpdatesWeightedAverageCost() {
        givenPrice("BNP.PA", "50.00");
        service.buy(1L, "BNP.PA", 4);   // 4 x 50 = 200
        givenPrice("BNP.PA", "80.00");
        service.buy(1L, "BNP.PA", 6);   // 6 x 80 = 480  -> avg = 680 / 10 = 68

        assertThat(portfolio.findPosition("BNP.PA").orElseThrow().getAverageCost())
                .isEqualByComparingTo("68.00");
    }

    @Test
    void buyFailsWhenNotEnoughCash() {
        givenPrice("MC.PA", "620.00");

        assertThatThrownBy(() -> service.buy(1L, "MC.PA", 2))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void sellBooksRealizedPnlAndClosesPosition() {
        givenPrice("AIR.PA", "100.00");
        service.buy(1L, "AIR.PA", 5);           // cash 500
        givenPrice("AIR.PA", "120.00");

        TransactionDto tx = service.sell(1L, "AIR.PA", 5);

        assertThat(tx.realizedPnl()).isEqualByComparingTo("100.00");
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo("1100.00");
        assertThat(portfolio.findPosition("AIR.PA")).isEmpty();
    }

    @Test
    void sellFailsWhenNotEnoughShares() {
        givenPrice("AIR.PA", "100.00");
        service.buy(1L, "AIR.PA", 2);

        assertThatThrownBy(() -> service.sell(1L, "AIR.PA", 3))
                .isInstanceOf(InsufficientQuantityException.class);
    }

    @Test
    void valuationUsesMarketPrices() {
        givenPrice("SAN.PA", "80.00");
        service.buy(1L, "SAN.PA", 10);          // cost 800, cash 200
        when(quoteClient.getQuotes(anyCollection())).thenReturn(Map.of("SAN.PA", quote("SAN.PA", "90.00")));

        PortfolioDto valuation = service.getValuation(1L);

        assertThat(valuation.investedValue()).isEqualByComparingTo("900.00");
        assertThat(valuation.totalValue()).isEqualByComparingTo("1100.00");
        assertThat(valuation.unrealizedPnl()).isEqualByComparingTo("100.00");
        assertThat(valuation.unrealizedPnlPercent()).isEqualByComparingTo("12.50");
    }

    @Test
    void unknownPortfolioThrowsNotFound() {
        when(portfolioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getValuation(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
