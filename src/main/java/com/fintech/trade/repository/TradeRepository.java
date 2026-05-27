package com.fintech.trade.repository;

import com.fintech.trade.entity.Trade;
import com.fintech.trade.enums.TradeState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Integer> {

    Page<Trade> findByState(TradeState state, Pageable pageable);

    java.util.List<Trade> findByStateAndExpiresAtBefore(TradeState state, java.time.LocalDateTime date);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Trade t WHERE t.sellerId = :userId OR t.buyerId = :userId ORDER BY CASE WHEN t.state = 'OPEN' THEN 1 ELSE 2 END, t.createdAt DESC")
    Page<Trade> findUserTradesCustomOrdered(@org.springframework.data.repository.query.Param("userId") Integer userId, Pageable pageable);
}
