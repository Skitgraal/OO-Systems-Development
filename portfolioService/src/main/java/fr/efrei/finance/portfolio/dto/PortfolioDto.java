package fr.efrei.finance.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Full portfolio valuation at current market prices. */
public record PortfolioDto(Long id,
                           String name,
                           String owner,
                           Instant createdAt,
                           BigDecimal cashBalance,
                           BigDecimal investedValue,
                           BigDecimal totalValue,
                           BigDecimal costBasis,
                           BigDecimal unrealizedPnl,
                           BigDecimal unrealizedPnlPercent,
                           Instant valuedAt,
                           List<PositionDto> positions) {
}
