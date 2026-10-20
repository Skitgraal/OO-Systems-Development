package fr.efrei.finance.quoteserver.exceptions;

public class InstrumentNotFoundException extends RuntimeException {

    public InstrumentNotFoundException(String symbol) {
        super("Unknown instrument: " + symbol);
    }
}
