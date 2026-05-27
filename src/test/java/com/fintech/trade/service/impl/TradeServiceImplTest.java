package com.fintech.trade.service.impl;

import com.fintech.trade.client.LedgerServiceClient;
import com.fintech.trade.client.WalletServiceClient;
import com.fintech.trade.dto.CreateTradeRequest;
import com.fintech.trade.dto.WalletResponse;
import com.fintech.trade.enums.Currency;
import com.fintech.trade.entity.Trade;
import com.fintech.trade.enums.TradeState;
import com.fintech.trade.exception.TradeStateException;
import com.fintech.trade.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private LedgerServiceClient ledgerServiceClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @InjectMocks
    private TradeServiceImpl tradeService;

    @Test
    @DisplayName("createTrade - success - calculates buy amount correctly")
    void createTrade_success() {
        CreateTradeRequest req = new CreateTradeRequest();
        req.setSellCurrency(Currency.USD);
        req.setBuyCurrency(Currency.EUR);
        req.setSellAmount(new BigDecimal("1000"));
        req.setExchangeRate(new BigDecimal("0.92"));
        req.setExpirationHours(48);

        WalletResponse wallet = new WalletResponse();
        wallet.setId(10);
        wallet.setActive(true);
        when(walletServiceClient.getOrCreateWallet(anyInt(), anyString())).thenReturn(wallet);

        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> {
            Trade t = inv.getArgument(0);
            t.setId(1);
            return t;
        });

        Trade result = tradeService.createTrade(1, req);

        assertNotNull(result);
        assertEquals(TradeState.OPEN, result.getState());
        assertEquals(new BigDecimal("920.00000000"), result.getBuyAmount());
        assertEquals(1, result.getSellerId());
    }

    @Test
    @DisplayName("createTrade - same currency - throws IllegalArgumentException")
    void createTrade_sameCurrency_throws() {
        CreateTradeRequest req = new CreateTradeRequest();
        req.setSellCurrency(Currency.USD);
        req.setBuyCurrency(Currency.USD);
        req.setSellAmount(new BigDecimal("100"));
        req.setExchangeRate(new BigDecimal("1.0"));

        WalletResponse wallet = new WalletResponse();
        wallet.setId(10);
        wallet.setActive(true);
        when(walletServiceClient.getOrCreateWallet(anyInt(), anyString())).thenReturn(wallet);

        assertThrows(IllegalArgumentException.class, () -> tradeService.createTrade(1, req));
    }

    @Test
    @DisplayName("acceptTrade - self trade - throws IllegalArgumentException")
    void acceptTrade_selfTrade_throws() {
        Trade trade = Trade.builder().id(1).sellerId(1).state(TradeState.OPEN).build();
        when(tradeRepository.findById(1)).thenReturn(Optional.of(trade));

        assertThrows(IllegalArgumentException.class, () -> tradeService.acceptTrade(1, 1));
    }

    @Test
    @DisplayName("acceptTrade - non-OPEN trade - throws TradeStateException")
    void acceptTrade_nonOpen_throws() {
        Trade trade = Trade.builder().id(1).sellerId(1).state(TradeState.COMPLETED).build();
        when(tradeRepository.findById(1)).thenReturn(Optional.of(trade));

        assertThrows(TradeStateException.class, () -> tradeService.acceptTrade(2, 1));
    }

    @Test
    @DisplayName("deleteTrade - not owner - throws IllegalArgumentException")
    void deleteTrade_notOwner_throws() {
        Trade trade = Trade.builder().id(1).sellerId(1).state(TradeState.OPEN).build();
        when(tradeRepository.findById(1)).thenReturn(Optional.of(trade));

        assertThrows(IllegalArgumentException.class, () -> tradeService.deleteTrade(99, 1));
    }
}
