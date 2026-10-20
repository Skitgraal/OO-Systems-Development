package fr.efrei.finance.portfolio.exceptions;

public class InsufficientQuantityException extends BusinessRuleException {

    public InsufficientQuantityException(String symbol, int requested, int held) {
        super("Cannot sell " + requested + " " + symbol + ": only " + held + " held");
    }
}
