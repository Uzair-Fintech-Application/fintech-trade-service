package com.fintech.trade.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class MarketOracleResponse {
    private String base;
    private Map<String, BigDecimal> rates;
}
