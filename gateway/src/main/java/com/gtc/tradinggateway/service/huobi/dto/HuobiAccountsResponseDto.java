package com.gtc.tradinggateway.service.huobi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by Valentyn Berezin on 14.04.18.
 */
@Data
public class HuobiAccountsResponseDto {

    private String status;
    private List<Account> data;

    public Account getPrimaryAccountOrThrow() {
        return data.stream().filter(it -> "working".equalsIgnoreCase(it.getState()) && "spot".equalsIgnoreCase(it.type))
                .findFirst().orElseThrow(() -> new IllegalStateException("No account"));
    }

    @Data
    public static class Account {

        private long id;
        private String type;
        private String state;

        @JsonProperty("user-id")
        private String userId;
    }
}
