package com.gtc.tradinggateway.aspect.rate;

import java.lang.annotation.*;

/**
 * Created by Valentyn Berezin on 24.02.18.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface IgnoreRateLimited {
}
