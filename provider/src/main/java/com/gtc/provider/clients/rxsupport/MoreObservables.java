package com.gtc.provider.clients.rxsupport;

import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.functions.Func1;

import javax.annotation.Nonnull;

@UtilityClass
public class MoreObservables {

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> Observable.Transformer<Object, T> filterAndMap(@Nonnull Class<T> clazz) {
        return observable -> observable
                .filter(clazz::isInstance)
                .map((Func1<Object, T>) o -> (T) o);
    }
}
