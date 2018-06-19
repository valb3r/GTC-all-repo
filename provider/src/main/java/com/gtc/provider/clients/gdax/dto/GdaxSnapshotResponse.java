package com.gtc.provider.clients.gdax.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by mikro on 06.01.2018.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GdaxSnapshotResponse extends GdaxResponse {

    private double[][] asks;
    private double[][] bids;

}
