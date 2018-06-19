package com.gtc.tradinggateway.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * Created by mikro on 15.02.2018.
 */
public class BasicHttpAuthHelper {

    public static Map<String, String> generateToken(String username, String password) {
        String key = username + ":" + password;
        String encoded = Base64.encodeBase64String(key.getBytes());
        return ImmutableMap.<String, String>builder()
                .put(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .build();
    }
}
