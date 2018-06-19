package com.gtc.opportunity.trader.service;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@UtilityClass
public class UuidGenerator {

    public String get() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
