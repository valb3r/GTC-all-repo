package com.gtc.provider.clients.gdax.dto;

import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by mikro on 06.01.2018.
 */
@EqualsAndHashCode(callSuper = true)
public class GdaxStringOp extends GdaxOp {

    protected List<String> channels;

    public GdaxStringOp(String type, List<String> productIds, List<String> channels) {
        this.type = type;
        this.channels = channels;
        this.productIds = productIds;
    }

}
