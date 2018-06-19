package com.gtc.tradinggateway.service.wex.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class BaseWexRequest {

    private final int nonce;
    private final String method;
}
