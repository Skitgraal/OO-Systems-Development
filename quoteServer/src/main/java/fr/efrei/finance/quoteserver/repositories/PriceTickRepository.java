package fr.efrei.finance.quoteserver.repositories;

import fr.efrei.finance.quoteserver.entities.PriceTick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface PriceTickRepository extends JpaRepository<PriceTick, Long> {

    long deleteByTimestampBefore(Instant limit);
}
