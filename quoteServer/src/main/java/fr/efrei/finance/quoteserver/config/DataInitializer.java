package fr.efrei.finance.quoteserver.config;

import fr.efrei.finance.quoteserver.entities.Instrument;
import fr.efrei.finance.quoteserver.repositories.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Seeds the database with a few CAC 40 stocks. Starting prices are indicative only.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedInstruments(InstrumentRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            List<Instrument> instruments = List.of(
                    new Instrument("MC.PA", "LVMH", "Consumer Goods", "EUR", 620.00),
                    new Instrument("TTE.PA", "TotalEnergies", "Energy", "EUR", 56.50),
                    new Instrument("BNP.PA", "BNP Paribas", "Banking", "EUR", 68.20),
                    new Instrument("GLE.PA", "Societe Generale", "Banking", "EUR", 40.10),
                    new Instrument("CS.PA", "AXA", "Insurance", "EUR", 37.80),
                    new Instrument("AIR.PA", "Airbus", "Aerospace", "EUR", 165.00),
                    new Instrument("SAN.PA", "Sanofi", "Healthcare", "EUR", 88.40),
                    new Instrument("OR.PA", "L'Oreal", "Consumer Goods", "EUR", 370.00),
                    new Instrument("SU.PA", "Schneider Electric", "Industrials", "EUR", 215.00),
                    new Instrument("CAP.PA", "Capgemini", "Technology", "EUR", 150.00)
            );
            repository.saveAll(instruments);
            log.info("Seeded {} instruments", instruments.size());
        };
    }
}
