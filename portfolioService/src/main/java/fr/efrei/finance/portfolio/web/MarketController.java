package fr.efrei.finance.portfolio.web;

import fr.efrei.finance.portfolio.dto.InstrumentDto;
import fr.efrei.finance.portfolio.dto.QuoteDto;
import fr.efrei.finance.portfolio.services.MarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST facade over the gRPC quote server.
 */
@RestController
@RequestMapping("/api")
public class MarketController {

    private static final Logger log = LoggerFactory.getLogger(MarketController.class);

    private final MarketService marketService;

    @Autowired
    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/instruments")
    public List<InstrumentDto> listInstruments() {
        log.info("GET /api/instruments");
        return marketService.listInstruments();
    }

    @GetMapping("/quotes/{symbol}")
    public QuoteDto getQuote(@PathVariable String symbol) {
        log.info("GET /api/quotes/{}", symbol);
        return marketService.getQuote(symbol);
    }

    /** Example: GET /api/quotes?symbols=BNP.PA,AIR.PA */
    @GetMapping("/quotes")
    public List<QuoteDto> getQuotes(@RequestParam List<String> symbols) {
        log.info("GET /api/quotes symbols={}", symbols);
        return marketService.getQuotes(symbols);
    }
}
