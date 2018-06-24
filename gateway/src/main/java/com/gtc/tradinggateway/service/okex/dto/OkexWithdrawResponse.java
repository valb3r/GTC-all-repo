package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 24.06.18.
 */
@Data
public class OkexWithdrawResponse implements IsResultOk {

    private String withdrawId;
    private boolean result;
    private long errorCode;
}
