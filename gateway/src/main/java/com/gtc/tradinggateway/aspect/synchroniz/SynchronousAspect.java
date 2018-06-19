package com.gtc.tradinggateway.aspect.synchroniz;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * Created by Valentyn Berezin on 22.03.18.
 */
@Aspect
@Component
public class SynchronousAspect {

    private final Map<String, IgnorableSemaphore> semaphores = new ConcurrentHashMap<>();

    @Around("execution(public * *(..)) && @within(ann) "
            + "&& @annotation(com.gtc.tradinggateway.aspect.synchroniz.LockAndProceed) "
            + "&& !@annotation(com.gtc.tradinggateway.aspect.synchroniz.Asynchronous)")
    public Object lockAndProceed(ProceedingJoinPoint joinPoint, Synchronous ann) throws Throwable {
        return doCompute(joinPoint, IgnorableSemaphore::acquireNonBlock);
    }

    @Around("execution(public * *(..)) && @within(ann) "
            + "&& !@annotation(com.gtc.tradinggateway.aspect.synchroniz.LockAndProceed) "
            + "&& !@annotation(com.gtc.tradinggateway.aspect.synchroniz.Asynchronous)")
    public Object synchronous(ProceedingJoinPoint joinPoint, Synchronous ann) throws Throwable {
        return doCompute(joinPoint, IgnorableSemaphore::acquireAndBlock);
    }

    private Object doCompute(ProceedingJoinPoint joinPoint, Function<IgnorableSemaphore, Integer> lockAcquirer)
            throws Throwable {
        IgnorableSemaphore semaphore = semaphores.computeIfAbsent(getKey(joinPoint), id -> new IgnorableSemaphore(1));
        int permits = lockAcquirer.apply(semaphore);
        try {
            return joinPoint.proceed();
        } finally {
            semaphore.release(permits);
        }
    }

    private String getKey(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getCanonicalName();
    }

    private static class IgnorableSemaphore extends Semaphore {

        IgnorableSemaphore(int i) {
            super(i);
        }

        int acquireAndBlock() {
            super.acquireUninterruptibly();
            return 1;
        }

        int acquireNonBlock() {
            super.reducePermits(1);
            return 1;
        }
    }
}
