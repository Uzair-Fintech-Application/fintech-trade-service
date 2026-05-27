package com.fintech.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateTradeRequest {

    @NotNull
    @DecimalMin(value = "0.00000001", message = "{validation.sell.amount.positive}")
    private BigDecimal sellAmount;

    @NotNull
    @DecimalMin(value = "0.000000000000000001", message = "{validation.exchange.rate.positive}")
    private BigDecimal exchangeRate;
}
