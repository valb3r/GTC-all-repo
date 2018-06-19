package com.gtc.tradinggateway.service.mock.dto;

import com.gtc.tradinggateway.meta.TradingCurrency;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Data
public class DepositDto {

    private final TradingCurrency currency;
    private final BigDecimal amount;
}
