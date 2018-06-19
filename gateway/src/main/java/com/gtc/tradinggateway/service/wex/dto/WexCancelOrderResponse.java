package com.gtc.tradinggateway.service.wex.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Getter
@Setter
@NoArgsConstructor
public class WexCancelOrderResponse extends BaseWexResponse<WexCancelOrderResponse.Value> {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Value {

        private long orderId;
    }
}
