package com.gtc.tradinggateway.service;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.meta.TradingCurrency;

import java.util.List;
import java.util.Optional;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
public interface ManageOrders extends ClientNamed {

    Optional<OrderDto> get(String id);

    List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to);

    void cancel(String id);
}
