package com.gtc.rxsupport;

import rx.Observable;
import rx.functions.Func1;

public class MoreObservables {

    @SuppressWarnings("unchecked")
    public static <T> Observable.Transformer<Object, T> filterAndMap(Class<T> clazz) {
        return observable -> observable
                .filter(o -> o != null && clazz.isInstance(o))
                .map((Func1<Object, T>) o -> (T) o);
    }
}
