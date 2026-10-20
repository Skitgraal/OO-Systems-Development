package fr.efrei.finance.quoteserver.services;

import fr.efrei.finance.quoteserver.entities.Instrument;
import fr.efrei.finance.quoteserver.exceptions.InstrumentNotFoundException;
import fr.efrei.finance.quoteserver.repositories.InstrumentRepository;
import fr.efrei.finance.quoteserver.repositories.PriceTickRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataServiceTest {

    private InstrumentRepository instrumentRepository;
    private PriceTickRepository priceTickRepository;
    private MarketDataService service;

    @BeforeEach
    void setUp() {
        instrumentRepository = mock(InstrumentRepository.class);
        priceTickRepository = mock(PriceTickRepository.class);
        service = new MarketDataService(instrumentRepository, priceTickRepository, 1.0);
    }

    @Test
    void getQuoteNormalizesSymbol() {
        Instrument bnp = new Instrument("BNP.PA", "BNP Paribas", "Banking", "EUR", 68.0);
        when(instrumentRepository.findById("BNP.PA")).thenReturn(Optional.of(bnp));

        assertThat(service.getQuote("  bnp.pa ")).isSameAs(bnp);
    }

    @Test
    void getQuoteThrowsWhenUnknown() {
        when(instrumentRepository.findById("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getQuote("xxx"))
                .isInstanceOf(InstrumentNotFoundException.class);
    }

    @Test
    void tickMovesPricesWithinBounds() {
        Instrument air = new Instrument("AIR.PA", "Airbus", "Aerospace", "EUR", 100.0);
        when(instrumentRepository.findAll()).thenReturn(List.of(air));

        service.simulateMarketTick();

        assertThat(air.getLastPrice()).isBetween(98.99, 101.01);
        verify(priceTickRepository, times(1)).save(any());
    }
}
