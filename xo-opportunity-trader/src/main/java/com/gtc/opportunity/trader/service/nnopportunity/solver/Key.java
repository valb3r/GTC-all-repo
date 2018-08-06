package com.gtc.opportunity.trader.service.nnopportunity.solver;

import com.gtc.meta.CurrencyPair;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class Key {

    private final String client;
    private final CurrencyPair pair;
}
