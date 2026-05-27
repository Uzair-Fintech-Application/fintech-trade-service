package com.fintech.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletResponse {

    private Integer id;
    private Integer userId;
    private String currency;
    private BigDecimal availableBalance;
    private Boolean systemWallet;
    private Boolean active;
}
