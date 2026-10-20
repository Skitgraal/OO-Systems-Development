package fr.efrei.finance.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteDto(String symbol,
                       String name,
                       String currency,
                       BigDecimal price,
                       BigDecimal previousClose,
                       BigDecimal changePercent,
                       Instant timestamp) {
}
