package com.gtc.tradinggateway.aspect.debug;

import com.gtc.tradinggateway.service.CreateOrder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 30.03.18.
 */
@Service
@Aspect
@ConditionalOnProperty(value = "NO_CREATE", havingValue = "true")
public class PreventCreationAspect {

    @Around("execution(public * com.gtc.tradinggateway..create(..))")
    public Object abort(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!(joinPoint.getThis() instanceof CreateOrder)) {
            return joinPoint.proceed();
        }

        throw new IllegalStateException("DEBUG disabled");
    }
}
