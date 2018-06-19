package com.gtc.tradinggateway.service.bitfinex;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.gtc.tradinggateway.config.BitfinexConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Created by mikro on 15.02.2018.
 */
@Service
@RequiredArgsConstructor
public class BitfinexEncryptionService {

    private static final String METHOD = "HmacSHA384";

    private final BitfinexConfig cfg;

    @SneakyThrows
    private String generatePayload(Object request) {
        String payload = cfg.getMapper().writeValueAsString(request);
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }

    @SneakyThrows
    public String generateSignature(String msg, String keyString) {
        SecretKeySpec key = new SecretKeySpec((keyString).getBytes(Charsets.UTF_8), METHOD);
        Mac mac = Mac.getInstance(METHOD);
        mac.init(key);

        return new String(Hex.encodeHex(mac.doFinal(msg.getBytes(Charsets.UTF_8))));
    }

    public HttpHeaders restHeaders(Object request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(signingHeaders(request));
        return headers;
    }

    private Map<String, String> signingHeaders(Object request) {
        String payload = generatePayload(request);
        return ImmutableMap.<String, String>builder()
                .put("accept", APPLICATION_JSON.toString())
                .put("content-type", APPLICATION_JSON.toString())
                .put("X-BFX-APIKEY", cfg.getPublicKey())
                .put("X-BFX-PAYLOAD", payload)
                .put("X-BFX-SIGNATURE",
                        generateSignature(payload, cfg.getSecretKey()).replace("-", "").toLowerCase())
                .build();
    }
}
