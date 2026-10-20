package fr.efrei.finance.portfolio.repositories;

import fr.efrei.finance.portfolio.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPortfolioIdOrderByExecutedAtDesc(Long portfolioId);

    void deleteByPortfolioId(Long portfolioId);
}
