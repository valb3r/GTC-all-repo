package com.gtc.tradinggateway.service.wex.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Getter
@Setter
@NoArgsConstructor
public class WexCreateResponse extends BaseWexResponse<WexCreateResponse.Value> {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Value {

        private double received;
        private double remains;
        private long orderId;
    }
}
