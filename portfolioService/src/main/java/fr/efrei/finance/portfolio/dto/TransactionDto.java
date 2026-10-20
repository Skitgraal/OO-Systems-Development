package fr.efrei.finance.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDto(Long id,
                             String type,
                             String symbol,
                             Integer quantity,
                             BigDecimal price,
                             BigDecimal amount,
                             BigDecimal realizedPnl,
                             Instant executedAt) {
}
