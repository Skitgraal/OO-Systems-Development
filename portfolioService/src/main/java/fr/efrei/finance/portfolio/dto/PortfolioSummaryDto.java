package fr.efrei.finance.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Light view of a portfolio, used in lists (no market data needed). */
public record PortfolioSummaryDto(Long id, String name, String owner, BigDecimal cashBalance,
                                  int positionCount, Instant createdAt) {
}
