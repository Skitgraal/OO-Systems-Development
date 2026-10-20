package fr.efrei.finance.portfolio.exceptions;

/** HTTP 422 - the request is valid but breaks a business rule (not enough cash, shares...). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
