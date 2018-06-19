package com.gtc.tradinggateway.service.wex;

import com.gtc.tradinggateway.config.WexConfig;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Service
@RequiredArgsConstructor
public class WexEncryptionService {

    private static final String KEY = "Key";
    private static final String SIGN = "Sign";

    private final WexConfig cfg;

    private static final String HMAC_SHA512 = "HmacSHA512";

    @SneakyThrows
    public <T> MultiValueMap<String, String> sign(T requestBody) {
        String body = FormHttpMessageToPojoConverter.pojoSerialize(cfg.getMapper(), requestBody, null);

        Mac macInst = Mac.getInstance(HMAC_SHA512);
        macInst.init(new SecretKeySpec(cfg.getSecretKey().getBytes(StandardCharsets.UTF_8), HMAC_SHA512));

        String signed = new String(Hex.encodeHex((macInst.doFinal(body.getBytes(StandardCharsets.UTF_8)))));

        HttpHeaders result = new HttpHeaders();
        result.add(KEY, cfg.getPublicKey());
        result.add(SIGN, signed);

        return result;
    }
}
