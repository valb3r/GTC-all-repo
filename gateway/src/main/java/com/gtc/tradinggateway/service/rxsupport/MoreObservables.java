package com.gtc.tradinggateway.service.rxsupport;

import rx.Observable;
import rx.functions.Func1;

import javax.annotation.Nonnull;

public class MoreObservables {

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> Observable.Transformer<Object, T> filterAndMap(@Nonnull Class<T> clazz) {
        return observable -> observable
                .filter(o -> o != null && clazz.isInstance(o))
                .map((Func1<Object, T>) o -> (T) o);
    }
}
