package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 24.06.18.
 * Might be incomplete - trade password/fee are not used
 */
@Data
public class OkexWithdrawRequest {

    private final String symbol;
    private final String withdrawAddress;
    private final BigDecimal withdrawAmount;
    private final String address = "address";
}
