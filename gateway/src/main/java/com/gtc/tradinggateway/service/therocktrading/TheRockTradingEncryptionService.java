package com.gtc.tradinggateway.service.therocktrading;

import com.google.common.collect.ImmutableMap;
import com.gtc.tradinggateway.config.TheRockTradingConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@RequiredArgsConstructor
public class TheRockTradingEncryptionService {

    private static final String METHOD = "HmacSHA512";

    private final TheRockTradingConfig cfg;

    @SneakyThrows
    private String generate(String absUrl, String nonce) {
        String preHash = nonce + absUrl;
        String secret = cfg.getSecretKey();
        Mac sha256hmac = Mac.getInstance(METHOD);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), METHOD);
        sha256hmac.init(secretKeySpec);
        return new String(Hex.encodeHex(sha256hmac.doFinal(preHash.getBytes())));
    }

    private Map<String, String> signingHeaders(String absUrl) {
        String timestamp = String.valueOf(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli());

        return ImmutableMap.<String, String>builder()
                .put("X-TRT-KEY", cfg.getPublicKey())
                .put("X-TRT-SIGN", generate(absUrl, timestamp))
                .put("X-TRT-NONCE", timestamp)
                .build();
    }

    public HttpHeaders restHeaders(String absUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
        headers.setAll(signingHeaders(absUrl));
        return headers;
    }

}
