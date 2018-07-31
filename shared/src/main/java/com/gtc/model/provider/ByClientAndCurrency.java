package com.gtc.model.provider;

import com.gtc.meta.CurrencyPair;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 13.06.18.
 */
@Data
@AllArgsConstructor
public class ByClientAndCurrency {

    protected String client;

    protected CurrencyPair pair;

    protected long timestamp = System.currentTimeMillis();

    public ByClientAndCurrency(String client, CurrencyPair pair) {
        this.client = client;
        this.pair = pair;
    }
}
