package fr.efrei.finance.portfolio.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends BusinessRuleException {

    public InsufficientFundsException(BigDecimal required, BigDecimal available) {
        super("Insufficient funds: " + required + " EUR required, " + available + " EUR available");
    }
}
