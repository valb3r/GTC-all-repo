package com.gtc.tradinggateway.service.hitbtc.dto;

import com.gtc.tradinggateway.util.UriFormatter;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by mikro on 13.02.2018.
 */
@Data
@RequiredArgsConstructor
public class HitbtcWithdrawRequestDto {

    private final String address;
    private final BigDecimal amount;
    private final String currency;

    @Override
    public String toString() {
        UriFormatter uri = new UriFormatter();
        uri.addToUri("currency", getCurrency());
        uri.addToUri("address", getAddress());
        uri.addToUri("amountFromOrig", String.valueOf(getAmount()));
        uri.addToUri("autoCommit", "true");
        return uri.toString();
    }
}
