package com.gtc.provider.clients.gdax.dto;

import lombok.Data;

import java.util.List;


/**
 * Created by mikro on 05.01.2018.
 */
@Data
public class GdaxOp {

    protected String type;
    protected List<String> productIds;
    protected List<Object> channels;

    public GdaxOp(String type, List<String> productIds, List<Object> channels) {
        this.type = type;
        this.channels = channels;
        this.productIds = productIds;
    }

    public GdaxOp() {}

}
