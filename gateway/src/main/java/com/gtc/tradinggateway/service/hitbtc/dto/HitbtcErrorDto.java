package com.gtc.tradinggateway.service.hitbtc.dto;

import lombok.Data;

/**
 * Created by mikro on 15.02.2018.
 */
@Data
public class HitbtcErrorDto {

    private ErrorBody error;

    @Data
    public static class ErrorBody {

        private int code;
        private String message;
    }
}
