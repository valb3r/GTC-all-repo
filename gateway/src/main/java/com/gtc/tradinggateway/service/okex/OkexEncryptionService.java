package com.gtc.tradinggateway.service.okex;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 23.06.18.
 */
@UtilityClass
public class OkexEncryptionService {

    private static final String API_KEY = "api_key";
    private static final String SECRET_KEY = "secret_key";
    private static final String SIGN = "sign";

    public static Map<String, String> generateSignature(String pub, String secret,
                                                  FormHttpMessageToPojoConverter.Parameters formRequest) {
        Map<String, String> keyValue = new HashMap<>(formRequest.getAsMap());
        keyValue.put(API_KEY, pub);
        List<String> keyValues = Lists.newArrayList(keyValue.keySet());
        keyValues.sort(String::compareTo);

        // secret key is last entry
        keyValue.put(SECRET_KEY, secret);
        keyValues.add(SECRET_KEY);

        StringBuilder resultRequest = new StringBuilder();
        keyValues.forEach(key -> {
            resultRequest.append(key);
            resultRequest.append("=");
            resultRequest.append(keyValue.get(key));
            resultRequest.append("&");
        });
        resultRequest.deleteCharAt(resultRequest.length() - 1);

        String sign = DigestUtils.md5Hex(resultRequest.toString().getBytes()).toUpperCase();

        return ImmutableMap.of(
                API_KEY, pub,
                SIGN, sign
        );
    }
}
