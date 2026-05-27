package com.fintech.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferRequest {

    private Integer fromWalletId;
    private Integer toWalletId;
    private BigDecimal amount;
    private String description;
    private Integer tradeId;
}
