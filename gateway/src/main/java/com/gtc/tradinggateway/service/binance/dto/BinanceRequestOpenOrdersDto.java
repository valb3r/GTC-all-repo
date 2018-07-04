package com.gtc.tradinggateway.service.binance.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by Valentyn Berezin on 04.07.18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BinanceRequestOpenOrdersDto extends BinanceRequestDto {

    private final String symbol;
}
