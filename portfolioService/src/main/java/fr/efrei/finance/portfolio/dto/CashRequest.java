package fr.efrei.finance.portfolio.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashRequest(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be > 0")
        @DecimalMax(value = "10000000", message = "amount must be <= 10,000,000")
        @Digits(integer = 8, fraction = 2, message = "amount must have at most 2 decimals") BigDecimal amount) {
}
