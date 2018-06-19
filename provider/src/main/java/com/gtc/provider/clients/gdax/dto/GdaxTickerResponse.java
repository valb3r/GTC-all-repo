package com.gtc.provider.clients.gdax.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by mikro on 06.01.2018.
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class GdaxTickerResponse extends GdaxResponse {

    private double price;

}
