package com.gtc.tradinggateway.aspect.rate;

import java.lang.annotation.*;

/**
 * Created by Valentyn Berezin on 20.02.18.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RateLimited {

    String ratePerMinute() default "";

    String minSeparationMs() default "";

    Mode mode() default Mode.METHOD;

    enum Mode {
        CLASS,
        METHOD
    }
}
