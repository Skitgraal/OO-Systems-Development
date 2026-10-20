package fr.efrei.finance.quoteserver.rpc;

import fr.efrei.finance.quote.grpc.Empty;
import fr.efrei.finance.quote.grpc.Instrument;
import fr.efrei.finance.quote.grpc.InstrumentList;
import fr.efrei.finance.quote.grpc.Quote;
import fr.efrei.finance.quote.grpc.QuoteList;
import fr.efrei.finance.quote.grpc.QuoteRequest;
import fr.efrei.finance.quote.grpc.QuoteServiceGrpc;
import fr.efrei.finance.quote.grpc.QuotesRequest;
import fr.efrei.finance.quoteserver.exceptions.InstrumentNotFoundException;
import fr.efrei.finance.quoteserver.services.MarketDataService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * gRPC endpoint (the "web" layer of this service).
 * Converts protobuf messages to/from entities and business exceptions to gRPC status codes.
 */
@Component
public class QuoteGrpcService extends QuoteServiceGrpc.QuoteServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(QuoteGrpcService.class);

    private final MarketDataService marketDataService;

    @Autowired
    public QuoteGrpcService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Override
    public void listInstruments(Empty request, StreamObserver<InstrumentList> responseObserver) {
        log.info("gRPC ListInstruments");
        handle(responseObserver, () -> InstrumentList.newBuilder()
                .addAllInstruments(marketDataService.listInstruments().stream()
                        .map(QuoteGrpcService::toInstrument)
                        .toList())
                .build());
    }

    @Override
    public void getQuote(QuoteRequest request, StreamObserver<Quote> responseObserver) {
        log.info("gRPC GetQuote symbol={}", request.getSymbol());
        handle(responseObserver, () -> toQuote(marketDataService.getQuote(request.getSymbol())));
    }

    @Override
    public void getQuotes(QuotesRequest request, StreamObserver<QuoteList> responseObserver) {
        log.info("gRPC GetQuotes symbols={}", request.getSymbolsList());
        handle(responseObserver, () -> QuoteList.newBuilder()
                .addAllQuotes(marketDataService.getQuotes(request.getSymbolsList()).stream()
                        .map(QuoteGrpcService::toQuote)
                        .toList())
                .build());
    }

    /** Runs the call and maps exceptions to proper gRPC errors (the gRPC "exception handler"). */
    private <T> void handle(StreamObserver<T> observer, Supplier<T> call) {
        try {
            observer.onNext(call.get());
            observer.onCompleted();
        } catch (InstrumentNotFoundException e) {
            log.warn(e.getMessage());
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in gRPC call", e);
            observer.onError(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        }
    }

    private static Instrument toInstrument(fr.efrei.finance.quoteserver.entities.Instrument entity) {
        return Instrument.newBuilder()
                .setSymbol(entity.getSymbol())
                .setName(entity.getName())
                .setSector(entity.getSector() == null ? "" : entity.getSector())
                .setCurrency(entity.getCurrency())
                .build();
    }

    private static Quote toQuote(fr.efrei.finance.quoteserver.entities.Instrument entity) {
        return Quote.newBuilder()
                .setSymbol(entity.getSymbol())
                .setName(entity.getName())
                .setCurrency(entity.getCurrency())
                .setPrice(entity.getLastPrice())
                .setPreviousClose(entity.getPreviousClose())
                .setChangePercent(Math.round(entity.getChangePercent() * 100.0) / 100.0)
                .setTimestamp(entity.getUpdatedAt().toEpochMilli())
                .build();
    }
}
