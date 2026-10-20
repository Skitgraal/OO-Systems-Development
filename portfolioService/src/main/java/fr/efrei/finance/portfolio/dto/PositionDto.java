package fr.efrei.finance.portfolio.dto;

import java.math.BigDecimal;

public record PositionDto(String symbol,
                          String name,
                          int quantity,
                          BigDecimal averageCost,
                          BigDecimal marketPrice,
                          BigDecimal dayChangePercent,
                          BigDecimal marketValue,
                          BigDecimal unrealizedPnl,
                          BigDecimal unrealizedPnlPercent,
                          BigDecimal weightPercent) {
}
