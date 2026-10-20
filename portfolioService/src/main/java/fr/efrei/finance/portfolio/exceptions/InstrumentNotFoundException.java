package fr.efrei.finance.portfolio.exceptions;

/** HTTP 404 - the quote server does not know this symbol. */
public class InstrumentNotFoundException extends ResourceNotFoundException {

    public InstrumentNotFoundException(String symbol) {
        super("Unknown instrument: " + symbol);
    }
}
