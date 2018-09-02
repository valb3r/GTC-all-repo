package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
public interface Fitter {

    FeeFitted after(OrderBook book, ClientConfig config);
    FeeFitted before(OrderBook book, ClientConfig config);
}
