package com.fintech.trade.dto;

import com.fintech.trade.entity.Trade;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EntityMapper {

    TradeResponse toTradeResponse(Trade trade);
}
