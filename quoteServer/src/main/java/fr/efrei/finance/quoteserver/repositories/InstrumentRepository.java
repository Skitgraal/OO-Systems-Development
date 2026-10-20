package fr.efrei.finance.quoteserver.repositories;

import fr.efrei.finance.quoteserver.entities.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {
}
