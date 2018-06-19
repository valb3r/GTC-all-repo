package com.gtc.tradinggateway.service.gdax;

import com.google.common.collect.ImmutableMap;
import com.gtc.tradinggateway.config.GdaxConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@Service
@RequiredArgsConstructor
public class GdaxEncryptionService {

    private static final String METHOD = "HmacSHA256";

    private final GdaxConfig cfg;

    @SneakyThrows
    public String generate(String requestPath, String method, String body, String timestamp) {
        String prehash = timestamp + method.toUpperCase() + requestPath + body;
        byte[] secretDecoded = Base64.getDecoder().decode(cfg.getSecretKey());
        SecretKeySpec keyspec = new SecretKeySpec(secretDecoded, METHOD);
        Mac sha256 = (Mac) Mac.getInstance(METHOD).clone();
        sha256.init(keyspec);
        return Base64.getEncoder().encodeToString(sha256.doFinal(prehash.getBytes()));
    }

    public Map<String, String> signingHeaders(String relativeUrl, String method, String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        return ImmutableMap.<String, String>builder()
                .put("accept", APPLICATION_JSON.toString())
                .put("content-type", APPLICATION_JSON.toString())
                .put("CB-ACCESS-KEY", cfg.getPublicKey())
                .put("CB-ACCESS-SIGN", generate(relativeUrl, method, body, timestamp))
                .put("CB-ACCESS-TIMESTAMP", timestamp)
                .put("CB-ACCESS-PASSPHRASE", cfg.getPassphrase())
                .build();
    }

    public HttpHeaders restHeaders(String relativeUrl, String method, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(signingHeaders(relativeUrl, method, body));
        return headers;
    }
}
