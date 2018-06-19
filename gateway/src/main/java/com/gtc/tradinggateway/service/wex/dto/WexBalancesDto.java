package com.gtc.tradinggateway.service.wex.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Getter
@Setter
@NoArgsConstructor
public class WexBalancesDto extends BaseWexResponse<WexBalancesDto.Value> {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Value {

        private Map<String, BigDecimal> funds;
    }
}
