package fr.efrei.finance.portfolio.exceptions;

/** HTTP 503 - the gRPC quote server cannot be reached. */
public class QuoteServiceUnavailableException extends RuntimeException {

    public QuoteServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
