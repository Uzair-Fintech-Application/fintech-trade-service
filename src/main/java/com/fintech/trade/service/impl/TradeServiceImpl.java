package com.fintech.trade.service.impl;

import com.fintech.trade.client.LedgerServiceClient;
import com.fintech.trade.client.WalletServiceClient;
import com.fintech.trade.constants.TradeConstants;
import com.fintech.trade.dto.*;
import com.fintech.trade.entity.Trade;
import com.fintech.trade.enums.TradeState;
import com.fintech.trade.exception.MarketDeviationException;
import com.fintech.trade.exception.ResourceNotFoundException;
import com.fintech.trade.exception.TradeStateException;
import com.fintech.trade.repository.TradeRepository;
import com.fintech.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final LedgerServiceClient ledgerServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final com.fintech.trade.client.MarketOracleClient marketOracleClient;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public Trade createTrade(Integer sellerId, CreateTradeRequest req) {
        log.info("TRADE_CREATE BEGIN | sellerId={} | req={}", sellerId, req);

        WalletResponse sellerWallet = walletServiceClient.getOrCreateWallet(sellerId, req.getSellCurrency().name());
        if (sellerWallet == null) {
            throw new ResourceNotFoundException(
                    msg("error.wallet.not.found", req.getSellCurrency(), sellerId));
        }
        if (Boolean.FALSE.equals(sellerWallet.getActive())) {
            throw new IllegalArgumentException(
                    msg("error.wallet.frozen", req.getSellCurrency()));
        }

        if (req.getSellCurrency() == req.getBuyCurrency()) {
            log.warn("TRADE_CREATE REJECTED | reason=SAME_CURRENCY | sellerId={}", sellerId);
            throw new IllegalArgumentException(msg("error.currency.same"));
        }

        try {
            MarketOracleResponse oracleResponse = marketOracleClient.getLatestRates(req.getSellCurrency().name());
            BigDecimal marketRate = oracleResponse.getRates().get(req.getBuyCurrency().name());
            if (marketRate != null) {
                BigDecimal lowerBound = marketRate.multiply(new BigDecimal("0.95"));
                BigDecimal upperBound = marketRate.multiply(new BigDecimal("1.05"));

                if (req.getExchangeRate().compareTo(lowerBound) < 0 || req.getExchangeRate().compareTo(upperBound) > 0) {
                    log.warn("TRADE_CREATE REJECTED | reason=MARKET_DEVIATION | requestedRate={} | marketRate={}", req.getExchangeRate(), marketRate);
                    throw new MarketDeviationException(msg("error.market.deviation", marketRate));
                }
            }
        } catch (MarketDeviationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ORACLE API FAILURE | Skipping bounds check. message={}", ex.getMessage());
        }

        BigDecimal buyAmount = req.getSellAmount()
                .multiply(req.getExchangeRate())
                .setScale(TradeConstants.CURRENCY_SCALE, RoundingMode.HALF_UP);

        Trade trade = tradeRepository.save(Trade.builder()
                .sellerId(sellerId)
                .sellCurrency(req.getSellCurrency())
                .buyCurrency(req.getBuyCurrency())
                .sellAmount(req.getSellAmount())
                .buyAmount(buyAmount)
                .exchangeRate(req.getExchangeRate())
                .state(TradeState.OPEN)
                .expiresAt(LocalDateTime.now().plusHours(req.getExpirationHours() != null ? req.getExpirationHours() : 48))
                .build());

        log.info("TRADE_CREATE OK | tradeId={} | sellerId={} | {}→{} | sellAmount={} | buyAmount={}",
                trade.getId(), sellerId, req.getSellCurrency(), req.getBuyCurrency(), req.getSellAmount(), buyAmount);
        return trade;
    }

    @Override
    @Transactional
    public Trade acceptTrade(Integer buyerId, Integer tradeId) {
        log.info("TRADE_ACCEPT BEGIN | buyerId={} | tradeId={}", buyerId, tradeId);

        Trade trade = findTrade(tradeId);
        if (trade.getState() != TradeState.OPEN) {
            log.warn("TRADE_ACCEPT REJECTED | reason=INVALID_STATE | tradeId={} | state={}", tradeId, trade.getState());
            throw new TradeStateException(msg("error.trade.not.open", trade.getState()));
        }
        if (trade.getSellerId().equals(buyerId)) {
            log.warn("TRADE_ACCEPT REJECTED | reason=SELF_TRADE | tradeId={}", tradeId);
            throw new IllegalArgumentException(msg("error.trade.self.accept"));
        }

        WalletResponse sellerSellWallet = walletServiceClient.getOrCreateWallet(
                trade.getSellerId(), trade.getSellCurrency().name());
        WalletResponse buyerBuyWallet = walletServiceClient.getOrCreateWallet(
                buyerId, trade.getBuyCurrency().name());
        WalletResponse buyerReceiveWallet = walletServiceClient.getOrCreateWallet(
                buyerId, trade.getSellCurrency().name());
        WalletResponse sellerReceiveWallet = walletServiceClient.getOrCreateWallet(
                trade.getSellerId(), trade.getBuyCurrency().name());

        if (sellerSellWallet == null || Boolean.FALSE.equals(sellerSellWallet.getActive())) {
            throw new IllegalArgumentException(msg("error.seller.wallet.frozen", trade.getSellCurrency()));
        }
        if (buyerBuyWallet == null || Boolean.FALSE.equals(buyerBuyWallet.getActive())) {
            throw new IllegalArgumentException(msg("error.buyer.wallet.frozen", trade.getBuyCurrency()));
        }
        if (buyerReceiveWallet == null || sellerReceiveWallet == null) {
            throw new IllegalArgumentException(msg("error.recipient.wallets.missing"));
        }

        WalletResponse systemSellWallet = walletServiceClient.getOrCreateWallet(TradeConstants.COMMISSION_USER_ID, trade.getSellCurrency().name());
        WalletResponse systemBuyWallet = walletServiceClient.getOrCreateWallet(TradeConstants.COMMISSION_USER_ID, trade.getBuyCurrency().name());

        BigDecimal userRate = new BigDecimal("0.999");
        BigDecimal buyerPayout = trade.getSellAmount().multiply(userRate).setScale(TradeConstants.CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal buyerCommission = trade.getSellAmount().subtract(buyerPayout);

        BigDecimal sellerPayout = trade.getBuyAmount().multiply(userRate).setScale(TradeConstants.CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal sellerCommission = trade.getBuyAmount().subtract(sellerPayout);

        BigDecimal minTransfer = new BigDecimal("0.01");

        ledgerServiceClient.transfer(TransferRequest.builder()
                .fromWalletId(sellerSellWallet.getId())
                .toWalletId(buyerReceiveWallet.getId())
                .amount(buyerPayout)
                .description(msg("trade.description.buyer.payout", tradeId))
                .tradeId(tradeId)
                .build());

        if (buyerCommission.compareTo(minTransfer) >= 0) {
            ledgerServiceClient.transfer(TransferRequest.builder()
                    .fromWalletId(sellerSellWallet.getId())
                    .toWalletId(systemSellWallet.getId())
                    .amount(buyerCommission)
                    .description(msg("trade.description.commission", tradeId))
                    .tradeId(tradeId)
                    .build());
        } else {
            log.info("TRADE_ACCEPT | Skipping buyer commission (below minimum): {}", buyerCommission);
        }

        ledgerServiceClient.transfer(TransferRequest.builder()
                .fromWalletId(buyerBuyWallet.getId())
                .toWalletId(sellerReceiveWallet.getId())
                .amount(sellerPayout)
                .description(msg("trade.description.seller.payout", tradeId))
                .tradeId(tradeId)
                .build());

        if (sellerCommission.compareTo(minTransfer) >= 0) {
            ledgerServiceClient.transfer(TransferRequest.builder()
                    .fromWalletId(buyerBuyWallet.getId())
                    .toWalletId(systemBuyWallet.getId())
                    .amount(sellerCommission)
                    .description(msg("trade.description.commission", tradeId))
                    .tradeId(tradeId)
                    .build());
        } else {
            log.info("TRADE_ACCEPT | Skipping seller commission (below minimum): {}", sellerCommission);
        }

        trade.setBuyerId(buyerId);
        trade.setState(TradeState.COMPLETED);
        Trade saved = tradeRepository.save(trade);

        log.info("TRADE_ACCEPT OK | tradeId={} | buyerId={} | sellerId={} | state=COMPLETED",
                tradeId, buyerId, trade.getSellerId());
        return saved;
    }

    @Override
    @Transactional
    public Trade updateTrade(Integer userId, Integer tradeId, UpdateTradeRequest req) {
        log.info("TRADE_UPDATE BEGIN | userId={} | tradeId={}", userId, tradeId);

        Trade trade = findTrade(tradeId);
        if (trade.getState() != TradeState.OPEN) {
            throw new TradeStateException(msg("error.trade.update.not.open"));
        }
        if (!trade.getSellerId().equals(userId)) {
            throw new IllegalArgumentException(msg("error.trade.update.not.seller"));
        }

        trade.setSellAmount(req.getSellAmount());
        trade.setExchangeRate(req.getExchangeRate());
        BigDecimal buyAmount = req.getSellAmount()
                .multiply(req.getExchangeRate())
                .setScale(TradeConstants.CURRENCY_SCALE, RoundingMode.HALF_UP);
        trade.setBuyAmount(buyAmount);

        Trade saved = tradeRepository.save(trade);
        log.info("TRADE_UPDATE OK | tradeId={} | newSellAmount={} | newBuyAmount={}", tradeId, req.getSellAmount(), buyAmount);
        return saved;
    }

    @Override
    @Transactional
    public void deleteTrade(Integer userId, Integer tradeId) {
        log.info("TRADE_DELETE BEGIN | userId={} | tradeId={}", userId, tradeId);

        Trade trade = findTrade(tradeId);
        if (trade.getState() != TradeState.OPEN) {
            throw new TradeStateException(msg("error.trade.delete.not.open"));
        }
        if (!trade.getSellerId().equals(userId)) {
            throw new IllegalArgumentException(msg("error.trade.delete.not.seller"));
        }

        tradeRepository.delete(trade);
        log.info("TRADE_DELETE OK | tradeId={} | by={}", tradeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Trade findTrade(Integer tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("error.trade.not.found", tradeId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trade> getOpenTrades(Pageable pageable) {
        return tradeRepository.findByState(TradeState.OPEN, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trade> getUserTrades(Integer userId, Pageable pageable) {
        return tradeRepository.findUserTradesCustomOrdered(userId, pageable);
    }

    @Override
    public MarketOracleResponse getOracleRates(String base) {
        return marketOracleClient.getLatestRates(base);
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }
}
