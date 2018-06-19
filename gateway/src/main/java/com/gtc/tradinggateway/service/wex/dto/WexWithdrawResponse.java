package com.gtc.tradinggateway.service.wex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Getter
@Setter
@NoArgsConstructor
public class WexWithdrawResponse extends BaseWexResponse<WexWithdrawResponse.Value> {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Value {

        @JsonProperty("tId")
        private long transactionId;

        @JsonProperty("amountSent")
        private double amountSent;
    }
}
