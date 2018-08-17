package com.gtc.opportunity.trader.service.xoopportunity.creation.precision;

import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

/**
 * Created by Valentyn Berezin on 29.04.18.
 */
@UtilityClass
public final class WarmupUtil {

    public <T, E extends Exception> T warmup(Supplier<T> supplier, int iterCount, Class<E> ignore) {
        T result = null;

        for (int i = 0; i < iterCount; ++i) {
            try {
                result = supplier.get();
            } catch (Exception ex) {
                if (!ignore.isInstance(ex)) {
                    throw ex;
                }
            }
        }

        return result;
    }
}
