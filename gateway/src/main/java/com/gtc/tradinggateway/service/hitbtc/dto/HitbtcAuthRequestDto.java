package com.gtc.tradinggateway.service.hitbtc.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by mikro on 15.02.2018.
 */
@Data
public class HitbtcAuthRequestDto {

    private String method = "login";
    private String id = "auth";
    private AuthBody params;

    @Getter
    @RequiredArgsConstructor
    public static class AuthBody {

        private final String algo = "BASIC";
        private final String pKey;
        private final String sKey;

        // weird names for proper field mapping
        public String getpKey() {
            return pKey;
        }

        // weird names for proper field mapping
        public String getsKey() {
            return sKey;
        }
    }

    public HitbtcAuthRequestDto(String pKey, String sKey) {
        params = new AuthBody(pKey, sKey);
    }
}
