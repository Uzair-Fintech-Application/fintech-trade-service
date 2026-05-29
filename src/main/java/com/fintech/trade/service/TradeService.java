package com.fintech.trade.service;

import com.fintech.trade.dto.CreateTradeRequest;
import com.fintech.trade.dto.MarketOracleResponse;
import com.fintech.trade.dto.UpdateTradeRequest;
import com.fintech.trade.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TradeService {

    Trade createTrade(Integer sellerId, CreateTradeRequest request);

    Trade acceptTrade(Integer buyerId, Integer tradeId);

    Trade updateTrade(Integer userId, Integer tradeId, UpdateTradeRequest request);

    void deleteTrade(Integer userId, Integer tradeId);

    Trade findTrade(Integer tradeId);

    Page<Trade> getOpenTrades(Pageable pageable);

    Page<Trade> getUserTrades(Integer userId, Pageable pageable);

    MarketOracleResponse getOracleRates(String base);
}

