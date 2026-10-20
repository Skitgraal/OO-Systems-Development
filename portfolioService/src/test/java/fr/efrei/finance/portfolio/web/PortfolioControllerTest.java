package fr.efrei.finance.portfolio.web;

import fr.efrei.finance.portfolio.exceptions.InsufficientFundsException;
import fr.efrei.finance.portfolio.exceptions.QuoteServiceUnavailableException;
import fr.efrei.finance.portfolio.exceptions.ResourceNotFoundException;
import fr.efrei.finance.portfolio.services.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that errors are returned with the right HTTP status and a JSON message.
 */
@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @Test
    void unknownPortfolioReturns404() throws Exception {
        when(portfolioService.getValuation(42L)).thenThrow(new ResourceNotFoundException("Portfolio 42 not found"));

        mockMvc.perform(get("/api/portfolios/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Portfolio 42 not found"));
    }

    @Test
    void invalidTradeReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/portfolios/1/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"\",\"quantity\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.symbol").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void insufficientFundsReturns422() throws Exception {
        when(portfolioService.buy(anyLong(), anyString(), anyInt()))
                .thenThrow(new InsufficientFundsException(new BigDecimal("1240.00"), new BigDecimal("100.00")));

        mockMvc.perform(post("/api/portfolios/1/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"MC.PA\",\"quantity\":2}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void quoteServerDownReturns503() throws Exception {
        when(portfolioService.getValuation(1L))
                .thenThrow(new QuoteServiceUnavailableException("down", null));

        mockMvc.perform(get("/api/portfolios/1"))
                .andExpect(status().isServiceUnavailable());
    }
}
