package com.gtc.provider.clients.gdax.dto;

import lombok.Data;

import java.util.List;


/**
 * Created by mikro on 05.01.2018.
 */

@Data
public class GdaxChannelItem {

    private String name;
    private List<String> productIds;

    public GdaxChannelItem(String name, List<String> productIds) {
        this.name = name;
        this.productIds = productIds;
    }

}
