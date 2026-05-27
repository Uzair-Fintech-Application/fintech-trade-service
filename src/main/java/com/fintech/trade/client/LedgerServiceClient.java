package com.fintech.trade.client;

import com.fintech.trade.dto.TransferRequest;
import com.fintech.trade.dto.TransferResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;

@Component
@Slf4j
public class LedgerServiceClient {

    private final RestTemplate restTemplate;
    private final String ledgerServiceUrl;
    private final MessageSource messageSource;

    public LedgerServiceClient(RestTemplate restTemplate,
                               @Value("${app.service.ledger-service-url}") String ledgerServiceUrl,
                               MessageSource messageSource) {
        this.restTemplate = restTemplate;
        this.ledgerServiceUrl = ledgerServiceUrl;
        this.messageSource = messageSource;
    }

    @CircuitBreaker(name = "ledgerService", fallbackMethod = "transferFallback")
    @Retry(name = "ledgerService")
    public TransferResponse transfer(TransferRequest request) {
        String url = ledgerServiceUrl + "/api/internal/transfer";
        log.info("LEDGER_CLIENT | calling {} | from={} | to={} | amount={} | tradeId={}",
                url, request.getFromWalletId(), request.getToWalletId(), request.getAmount(), request.getTradeId());
        try {
            TransferResponse response = restTemplate.postForObject(url, request, TransferResponse.class);
            log.info("LEDGER_CLIENT | OK | refId={}", response != null ? response.getReferenceId() : "null");
            return response;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorMessage = messageSource.getMessage("error.transfer.failed", null, Locale.getDefault());
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
                if (root.has("message")) {
                    errorMessage = root.get("message").asText();
                } else if (root.has("details")) {
                    errorMessage = root.get("details").toString();
                }
            } catch (Exception ex) {
                log.debug("LEDGER_CLIENT | Failed to parse error response body | error={}", ex.getMessage());
            }
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private TransferResponse transferFallback(TransferRequest request, Throwable t) {
        log.error("LEDGER_CLIENT | CIRCUIT OPEN | from={} | to={} | error={}",
                request.getFromWalletId(), request.getToWalletId(), t.getMessage());
        throw new RuntimeException(messageSource.getMessage("error.ledger.unavailable", null, Locale.getDefault()));
    }
}
