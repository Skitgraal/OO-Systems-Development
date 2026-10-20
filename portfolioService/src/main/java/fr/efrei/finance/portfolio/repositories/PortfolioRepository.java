package fr.efrei.finance.portfolio.repositories;

import fr.efrei.finance.portfolio.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findAllByOrderByCreatedAtDesc();
}
