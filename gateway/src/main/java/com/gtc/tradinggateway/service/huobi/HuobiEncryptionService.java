package com.gtc.tradinggateway.service.huobi;

import com.gtc.tradinggateway.config.HuobiConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuobiEncryptionService {

    public static final String METHOD = "HmacSHA256";

    private final HuobiConfig cfg;

    @SneakyThrows
    public String generate(HttpMethod method, String url, String params) {
        String message = method.name() + "\napi.huobi.pro\n" + url + "\n" + params;
        String secret = cfg.getSecretKey();
        Mac sha256hmac = Mac.getInstance(METHOD);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), METHOD);
        sha256hmac.init(secretKeySpec);
        return Base64.encodeBase64String(sha256hmac.doFinal(message.getBytes()));
    }

    public HttpHeaders restHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE);
        headers.add(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:53.0) Gecko/20100101 Firefox/53.0)"
        );
        return headers;
    }

    public HttpHeaders restHeaders(HttpMethod method) {
        HttpHeaders headers = restHeaders();
        if (method == HttpMethod.POST) {
            headers.add(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE);
            headers.remove(HttpHeaders.CONTENT_TYPE);
            headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
        }
        return headers;
    }
}
