package com.gtc.provider.clients.exx.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mikro on 16.01.2018.
 */
@Data
public class ExxOrderBookResponse {

    private List<Double[]> asks = new ArrayList<>();
    private List<Double[]> bids = new ArrayList<>();
}
