package fr.efrei.finance.portfolio.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePortfolioRequest(
        @NotBlank(message = "name is required") @Size(max = 60) String name,
        @NotBlank(message = "owner is required") @Size(max = 60) String owner,
        @PositiveOrZero(message = "initialCash must be >= 0")
        @DecimalMax(value = "10000000", message = "initialCash must be <= 10,000,000") BigDecimal initialCash) {
}
