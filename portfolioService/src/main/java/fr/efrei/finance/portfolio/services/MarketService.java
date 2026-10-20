package fr.efrei.finance.portfolio.services;

import fr.efrei.finance.portfolio.client.QuoteClient;
import fr.efrei.finance.portfolio.dto.InstrumentDto;
import fr.efrei.finance.portfolio.dto.QuoteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Market data use cases, delegated to the gRPC quote server.
 */
@Service
public class MarketService {

    private final QuoteClient quoteClient;

    @Autowired
    public MarketService(QuoteClient quoteClient) {
        this.quoteClient = quoteClient;
    }

    public List<InstrumentDto> listInstruments() {
        List<InstrumentDto> instruments = new ArrayList<>(quoteClient.listInstruments());
        instruments.sort(Comparator.comparing(InstrumentDto::symbol));
        return instruments;
    }

    public QuoteDto getQuote(String symbol) {
        return quoteClient.getQuote(symbol);
    }

    public List<QuoteDto> getQuotes(Collection<String> symbols) {
        List<QuoteDto> quotes = new ArrayList<>(quoteClient.getQuotes(symbols).values());
        quotes.sort(Comparator.comparing(QuoteDto::symbol));
        return quotes;
    }
}
