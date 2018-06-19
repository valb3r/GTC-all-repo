package com.gtc.provider.clients.okex.dto;

import lombok.Data;

/**
 * Created by mikro on 12.01.2018.
 */
@Data
public class OkexOrderBookResponse {

    private double[][] asks;
    private double[][] bids;

}
