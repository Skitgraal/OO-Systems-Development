package fr.efrei.finance.portfolio.web;

import fr.efrei.finance.portfolio.dto.CashRequest;
import fr.efrei.finance.portfolio.dto.CreatePortfolioRequest;
import fr.efrei.finance.portfolio.dto.PortfolioDto;
import fr.efrei.finance.portfolio.dto.PortfolioSummaryDto;
import fr.efrei.finance.portfolio.dto.TradeRequest;
import fr.efrei.finance.portfolio.dto.TransactionDto;
import fr.efrei.finance.portfolio.services.PortfolioService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST API for portfolios. Stateless: every request carries everything it needs
 * (no HTTP session); all state lives in the database.
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioSummaryDto> listPortfolios() {
        log.info("GET /api/portfolios");
        return portfolioService.listPortfolios();
    }

    @PostMapping
    public ResponseEntity<PortfolioSummaryDto> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
        log.info("POST /api/portfolios name='{}' owner='{}'", request.name(), request.owner());
        PortfolioSummaryDto created = portfolioService.createPortfolio(
                request.name(), request.owner(), request.initialCash());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public PortfolioDto getPortfolio(@PathVariable Long id) {
        log.info("GET /api/portfolios/{}", id);
        return portfolioService.getValuation(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePortfolio(@PathVariable Long id) {
        log.info("DELETE /api/portfolios/{}", id);
        portfolioService.deletePortfolio(id);
    }

    @PostMapping("/{id}/deposit")
    public PortfolioSummaryDto deposit(@PathVariable Long id, @Valid @RequestBody CashRequest request) {
        log.info("POST /api/portfolios/{}/deposit amount={}", id, request.amount());
        return portfolioService.deposit(id, request.amount());
    }

    @PostMapping("/{id}/withdraw")
    public PortfolioSummaryDto withdraw(@PathVariable Long id, @Valid @RequestBody CashRequest request) {
        log.info("POST /api/portfolios/{}/withdraw amount={}", id, request.amount());
        return portfolioService.withdraw(id, request.amount());
    }

    @PostMapping("/{id}/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto buy(@PathVariable Long id, @Valid @RequestBody TradeRequest request) {
        log.info("POST /api/portfolios/{}/buy {} x {}", id, request.quantity(), request.symbol());
        return portfolioService.buy(id, request.symbol(), request.quantity());
    }

    @PostMapping("/{id}/sell")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto sell(@PathVariable Long id, @Valid @RequestBody TradeRequest request) {
        log.info("POST /api/portfolios/{}/sell {} x {}", id, request.quantity(), request.symbol());
        return portfolioService.sell(id, request.symbol(), request.quantity());
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionDto> getTransactions(@PathVariable Long id) {
        log.info("GET /api/portfolios/{}/transactions", id);
        return portfolioService.getTransactions(id);
    }
}
