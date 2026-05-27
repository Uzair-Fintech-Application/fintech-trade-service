package com.fintech.trade.entity;

import com.fintech.trade.enums.Currency;
import com.fintech.trade.enums.TradeState;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "buyer_id")
    private Integer buyerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sell_currency", nullable = false)
    private Currency sellCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "buy_currency", nullable = false)
    private Currency buyCurrency;

    @Column(name = "sell_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal sellAmount;

    @Column(name = "buy_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal buyAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 30, scale = 18)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeState state;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
