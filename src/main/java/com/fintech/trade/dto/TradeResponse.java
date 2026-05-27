package com.fintech.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeResponse {

    private Integer id;
    private Integer sellerId;
    private Integer buyerId;
    private String sellCurrency;
    private String buyCurrency;
    private BigDecimal sellAmount;
    private BigDecimal buyAmount;
    private BigDecimal exchangeRate;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
