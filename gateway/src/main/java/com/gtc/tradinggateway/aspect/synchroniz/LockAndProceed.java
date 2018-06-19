package com.gtc.tradinggateway.aspect.synchroniz;

import java.lang.annotation.*;

/**
 * Locks until completion but does not await for lock to be available.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface LockAndProceed {
}
