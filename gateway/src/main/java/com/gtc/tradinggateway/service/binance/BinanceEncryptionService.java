package com.gtc.tradinggateway.service.binance;

import com.google.common.collect.ImmutableMap;
import com.gtc.tradinggateway.config.BinanceConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

/**
 * Created by mikro on 24.01.2018.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceEncryptionService {

    private static final String METHOD = "HmacSHA256";
    private static final String APIKEY_HEADER = "X-MBX-APIKEY";

    private final BinanceConfig cfg;

    @SneakyThrows
    public String generate(String message) {
        String secret = cfg.getSecretKey();
        Mac sha256hmac = Mac.getInstance(METHOD);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), METHOD);
        sha256hmac.init(secretKeySpec);
        return new String(Hex.encodeHex(sha256hmac.doFinal(message.getBytes())));
    }

    public Map<String, String> signingHeaders() {
        return ImmutableMap.<String, String>builder()
                .put(APIKEY_HEADER, cfg.getPublicKey())
                .build();
    }

    public HttpHeaders restHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(signingHeaders());
        return headers;
    }
}
