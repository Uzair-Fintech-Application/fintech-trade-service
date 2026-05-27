package com.fintech.trade.service;

import com.fintech.trade.entity.Trade;
import com.fintech.trade.enums.TradeState;
import com.fintech.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeReaperService {

    private final TradeRepository tradeRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void reapExpiredTrades() {
        LocalDateTime now = LocalDateTime.now();
        List<Trade> expiredTrades = tradeRepository.findByStateAndExpiresAtBefore(TradeState.OPEN, now);

        if (!expiredTrades.isEmpty()) {
            log.info("REAPER | Found {} expired trades. Marking as EXPIRED.", expiredTrades.size());
            for (Trade trade : expiredTrades) {
                trade.setState(TradeState.EXPIRED);
                tradeRepository.save(trade);
            }
            log.info("REAPER | Expiration sweep complete.");
        }
    }
}
