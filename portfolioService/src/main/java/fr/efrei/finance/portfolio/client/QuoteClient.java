package fr.efrei.finance.portfolio.client;

import fr.efrei.finance.portfolio.dto.InstrumentDto;
import fr.efrei.finance.portfolio.dto.QuoteDto;
import fr.efrei.finance.portfolio.exceptions.InstrumentNotFoundException;
import fr.efrei.finance.portfolio.exceptions.QuoteServiceUnavailableException;
import fr.efrei.finance.quote.grpc.Empty;
import fr.efrei.finance.quote.grpc.Quote;
import fr.efrei.finance.quote.grpc.QuoteRequest;
import fr.efrei.finance.quote.grpc.QuoteServiceGrpc;
import fr.efrei.finance.quote.grpc.QuotesRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * gRPC client of the quote server. Hides protobuf/gRPC details from the business layer:
 * it returns plain DTOs and throws business exceptions.
 */
@Component
public class QuoteClient {

    private static final Logger log = LoggerFactory.getLogger(QuoteClient.class);

    private final QuoteServiceGrpc.QuoteServiceBlockingStub stub;
    private final long deadlineMs;

    @Autowired
    public QuoteClient(QuoteServiceGrpc.QuoteServiceBlockingStub stub,
                       @Value("${quote-service.deadline-ms:3000}") long deadlineMs) {
        this.stub = stub;
        this.deadlineMs = deadlineMs;
    }

    public List<InstrumentDto> listInstruments() {
        return call("ListInstruments", null, () -> stubWithDeadline()
                .listInstruments(Empty.getDefaultInstance())
                .getInstrumentsList().stream()
                .map(i -> new InstrumentDto(i.getSymbol(), i.getName(), i.getSector(), i.getCurrency()))
                .toList());
    }

    public QuoteDto getQuote(String symbol) {
        String normalized = normalize(symbol);
        return call("GetQuote", normalized, () -> toDto(stubWithDeadline()
                .getQuote(QuoteRequest.newBuilder().setSymbol(normalized).build())));
    }

    /** Returns quotes indexed by symbol. Unknown symbols are simply absent from the map. */
    public Map<String, QuoteDto> getQuotes(Collection<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }
        return call("GetQuotes", null, () -> stubWithDeadline()
                .getQuotes(QuotesRequest.newBuilder().addAllSymbols(symbols).build())
                .getQuotesList().stream()
                .map(QuoteClient::toDto)
                .collect(Collectors.toMap(QuoteDto::symbol, Function.identity())));
    }

    private QuoteServiceGrpc.QuoteServiceBlockingStub stubWithDeadline() {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    /** Translates gRPC errors into business exceptions. */
    private <T> T call(String method, String symbol, Supplier<T> grpcCall) {
        try {
            log.debug("gRPC call {} symbol={}", method, symbol);
            return grpcCall.get();
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            if (code == Status.Code.NOT_FOUND) {
                throw new InstrumentNotFoundException(symbol);
            }
            if (code == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(e.getStatus().getDescription());
            }
            log.error("Quote server call {} failed with status {}", method, code);
            throw new QuoteServiceUnavailableException("Quote service unavailable (" + code + ")", e);
        }
    }

    private static String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be empty");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static QuoteDto toDto(Quote quote) {
        return new QuoteDto(
                quote.getSymbol(),
                quote.getName(),
                quote.getCurrency(),
                money(quote.getPrice()),
                money(quote.getPreviousClose()),
                money(quote.getChangePercent()),
                Instant.ofEpochMilli(quote.getTimestamp()));
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
