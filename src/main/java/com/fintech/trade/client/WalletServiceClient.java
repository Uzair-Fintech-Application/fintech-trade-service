package com.fintech.trade.client;

import com.fintech.trade.dto.WalletResponse;
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
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;
    private final MessageSource messageSource;

    public WalletServiceClient(RestTemplate restTemplate,
                               @Value("${app.service.wallet-service-url}") String walletServiceUrl,
                               MessageSource messageSource) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
        this.messageSource = messageSource;
    }

    @CircuitBreaker(name = "walletService", fallbackMethod = "getOrCreateWalletFallback")
    @Retry(name = "walletService")
    public WalletResponse getOrCreateWallet(Integer userId, String currency) {
        String url = walletServiceUrl + "/api/wallets/internal/by-user-and-currency?userId=" + userId + "&currency=" + currency;
        log.info("WALLET_CLIENT | calling {} | userId={} | currency={}", url, userId, currency);
        WalletResponse response = restTemplate.getForObject(url, WalletResponse.class);
        log.info("WALLET_CLIENT | OK | walletId={}", response != null ? response.getId() : "null");
        return response;
    }

    private WalletResponse getOrCreateWalletFallback(Integer userId, String currency, Throwable t) {
        log.error("WALLET_CLIENT | CIRCUIT OPEN | userId={} | currency={} | error={}", userId, currency, t.getMessage());
        throw new RuntimeException(messageSource.getMessage("error.wallet.unavailable", null, Locale.getDefault()));
    }
}
