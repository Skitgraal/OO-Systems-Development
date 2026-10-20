package fr.efrei.finance.portfolio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TradeRequest(
        @NotBlank(message = "symbol is required") String symbol,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be > 0")
        @Max(value = 1_000_000, message = "quantity must be <= 1,000,000") Integer quantity) {
}
