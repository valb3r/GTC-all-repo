package com.gtc.tradinggateway.service.hitbtc;

import com.gtc.tradinggateway.config.HitbtcConfig;
import com.gtc.tradinggateway.util.BasicHttpAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * Created by mikro on 12.02.2018.
 */
@Service
@RequiredArgsConstructor
public class HitbtcEncryptionService {

    private final HitbtcConfig cfg;

    public HttpHeaders restHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(BasicHttpAuthHelper.generateToken(cfg.getPublicKey(), cfg.getSecretKey()));
        return headers;
    }
}
