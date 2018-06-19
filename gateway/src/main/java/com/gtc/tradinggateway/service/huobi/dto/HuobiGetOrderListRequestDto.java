package com.gtc.tradinggateway.service.huobi.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

/**
 * Created by Valentyn Berezin on 14.04.18.
 */
@Getter
@JsonPropertyOrder({"AccessKeyId", "SignatureMethod", "SignatureVersion", "Timestamp", "orderId", "states", "symbol"})
public class HuobiGetOrderListRequestDto extends HuobiRequestDto {

    private final String symbol;
    private final String states = "pre-submitted,submitted,partial-filled";

    public HuobiGetOrderListRequestDto(String accessKeyId, String symbol) {
        super(accessKeyId);
        this.symbol = symbol;
    }
}
