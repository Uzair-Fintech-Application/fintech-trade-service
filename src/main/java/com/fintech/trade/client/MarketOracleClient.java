package com.fintech.trade.client;

import com.fintech.trade.dto.MarketOracleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketOracleClient {

    private final RestTemplate restTemplate;
    private final String ORACLE_URL = "https://api.exchangerate-api.com/v4/latest/";

    public MarketOracleResponse getLatestRates(String baseCurrency) {
        String url = ORACLE_URL + baseCurrency;
        log.info("ORACLE_CLIENT | Fetching live rates for {}", baseCurrency);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
        
        org.springframework.http.ResponseEntity<MarketOracleResponse> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                MarketOracleResponse.class
        );
        return response.getBody();
    }
}
