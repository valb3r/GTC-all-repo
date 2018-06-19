package com.gtc.tradinggateway.service.wex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Getter
@Setter
@NoArgsConstructor
public class BaseWexResponse<T> {

    private short success;

    private String error;

    @JsonProperty("return")
    private T ret;

    public boolean isOk() {
        return success == 1;
    }

    public void selfAssert() {

        if (success != 1) {
            throw new IllegalStateException(error);
        }
    }
}
