package com.fintech.trade.constants;

public final class TradeConstants {

    private TradeConstants() {
    }

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_HEADER = "Authorization";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";

    public static final int CURRENCY_SCALE = 8;
    public static final int RATE_SCALE = 18;

    /** System liquidity pool user ID — matches wallet-service SYSTEM_USER_ID */
    public static final int SYSTEM_USER_ID = 1;

    /** Commission wallet user ID — matches wallet-service COMMISSION_USER_ID */
    public static final int COMMISSION_USER_ID = 2;
}
