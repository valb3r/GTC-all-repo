package com.gtc.opportunity.trader.cqe.domain;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import lombok.*;
import lombok.experimental.Delegate;

import java.util.concurrent.atomic.AtomicLong;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

@Getter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id", "version"})
public class FullCrossMarketOpportunity {

    private static final AtomicLong VERSION_GENERATOR = new AtomicLong();

    public static final SimpleAttribute<FullCrossMarketOpportunity, String> A_ID =
            attribute("id", FullCrossMarketOpportunity::getId);

    private final String id;

    private final long version = VERSION_GENERATOR.getAndIncrement();

    @Delegate
    private final CrossMarketOpportunity opportunity;

    // price asc order
    private final Histogram[] sell;

    // price desc order
    private final Histogram[] buy;

    public static String calculateId(CrossMarketOpportunity opportunity) {
        return opportunity.getClientFrom() + opportunity.getClientTo()
                + opportunity.getCurrencyFrom() + opportunity.getCurrencyTo();
    }

    @Data
    @RequiredArgsConstructor
    public static class Histogram {

        private final double minPrice;
        private final double maxPrice;
        private final double amount;
    }
}
