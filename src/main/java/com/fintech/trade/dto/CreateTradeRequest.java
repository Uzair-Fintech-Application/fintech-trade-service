package com.fintech.trade.dto;

import com.fintech.trade.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTradeRequest {

    @NotNull
    private Currency sellCurrency;

    @NotNull
    private Currency buyCurrency;

    @NotNull
    @DecimalMin("0.00000001")
    private BigDecimal sellAmount;

    @NotNull
    @DecimalMin("0.00000001")
    private BigDecimal exchangeRate;

    private Integer expirationHours;
}
