package com.gtc.tradinggateway.aspect.rate;

import com.google.common.base.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.gtc.tradinggateway.aspect.rate.RateLimited.Mode.CLASS;

/**
 * Created by Valentyn Berezin on 20.02.18.
 */
@Aspect
@Component
public class RateLimitingAspect {

    private final Map<String, TokenBucket> limiters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> minIntervalLimiters = new ConcurrentHashMap<>();

    private final EmbeddedValueResolver resolver;

    public RateLimitingAspect(ConfigurableBeanFactory beanFactory) {
        this.resolver = new EmbeddedValueResolver(beanFactory);
    }

    @Around("execution(public * *(..)) && (@annotation(ann) || @within(ann)) " +
            "&& !@annotation(com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited ann) throws Throwable {
        Method method = getMethod(joinPoint);
        String key = getKey(method, ann);
        int tokens = Integer.parseInt(resolver.resolveStringValue(ann.ratePerMinute()));

        boolean minInterval = minIntervalAcquire(key, ann);
        boolean acquired = limiters.computeIfAbsent(key, id ->
                        TokenBuckets.builder()
                        .withCapacity(tokens)
                        .withFixedIntervalRefillStrategy(tokens, 1, TimeUnit.MINUTES)
                        .build()
        ).tryConsume();

        if (!acquired || !minInterval) {
            throw new RateTooHighException("Rate limiting");
        }

        return joinPoint.proceed();
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }

    private String getKey(Method method, RateLimited ann) {
        // method level annotation overrides within
        if (CLASS.equals(ann.mode()) && null == method.getAnnotation(RateLimited.class)) {
            return method.getDeclaringClass().getCanonicalName();
        }

        return method.getDeclaringClass().getName() + "." + method.getName()
                + "("
                + Arrays.stream(method.getGenericParameterTypes())
                .map(Type::getTypeName)
                .collect(Collectors.joining(","))
                + ")";
    }

    private boolean minIntervalAcquire(String key, RateLimited ann) {
        if (Strings.isNullOrEmpty(ann.minSeparationMs())) {
            return true;
        }

        int minMs = Integer.parseInt(resolver.resolveStringValue(ann.minSeparationMs()));
        AtomicBoolean acquired = new AtomicBoolean();
        minIntervalLimiters.compute(key, (id, oldTime) -> {
            LocalDateTime now = LocalDateTime.now();
            if (null == oldTime || ChronoUnit.MILLIS.between(oldTime, now) >= minMs) {
                acquired.set(true);
                return LocalDateTime.now();
            }

            return null;
        });

        return acquired.get();
    }
}
