package fr.efrei.finance.quoteserver.services;

import fr.efrei.finance.quoteserver.entities.Instrument;
import fr.efrei.finance.quoteserver.entities.PriceTick;
import fr.efrei.finance.quoteserver.exceptions.InstrumentNotFoundException;
import fr.efrei.finance.quoteserver.repositories.InstrumentRepository;
import fr.efrei.finance.quoteserver.repositories.PriceTickRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Business layer of the quote server: reads instruments and simulates market moves.
 * Prices follow a simple random walk (they are NOT real market data).
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private static final Duration HISTORY_RETENTION = Duration.ofHours(1);

    private final InstrumentRepository instrumentRepository;
    private final PriceTickRepository priceTickRepository;
    private final double maxMovePercent;

    @Autowired
    public MarketDataService(InstrumentRepository instrumentRepository,
                             PriceTickRepository priceTickRepository,
                             @Value("${market.max-move-percent:0.8}") double maxMovePercent) {
        this.instrumentRepository = instrumentRepository;
        this.priceTickRepository = priceTickRepository;
        this.maxMovePercent = maxMovePercent;
    }

    @Transactional(readOnly = true)
    public List<Instrument> listInstruments() {
        return instrumentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Instrument getQuote(String symbol) {
        String normalized = normalize(symbol);
        return instrumentRepository.findById(normalized)
                .orElseThrow(() -> new InstrumentNotFoundException(normalized));
    }

    /** Returns the quotes of the known symbols; unknown symbols are ignored. */
    @Transactional(readOnly = true)
    public List<Instrument> getQuotes(Collection<String> symbols) {
        List<String> normalized = symbols.stream().map(this::normalize).distinct().toList();
        List<Instrument> found = new ArrayList<>(instrumentRepository.findAllById(normalized));
        if (found.size() < normalized.size()) {
            log.warn("Some requested symbols are unknown: {}", normalized);
        }
        return found;
    }

    /** Moves every price a little bit, at a fixed rate. */
    @Scheduled(fixedRateString = "${market.tick-interval-ms:3000}")
    @Transactional
    public void simulateMarketTick() {
        Instant now = Instant.now();
        List<Instrument> instruments = instrumentRepository.findAll();
        for (Instrument instrument : instruments) {
            double movePercent = ThreadLocalRandom.current().nextDouble(-maxMovePercent, maxMovePercent);
            double newPrice = round2(instrument.getLastPrice() * (1 + movePercent / 100.0));
            instrument.setLastPrice(Math.max(newPrice, 0.01));
            priceTickRepository.save(new PriceTick(instrument.getSymbol(), instrument.getLastPrice(), now));
        }
        priceTickRepository.deleteByTimestampBefore(now.minus(HISTORY_RETENTION));
        log.debug("Market tick applied to {} instruments", instruments.size());
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be empty");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
