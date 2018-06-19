package com.gtc.opportunity.trader.aop;

import com.gtc.model.gateway.BaseMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Created by Valentyn Berezin on 01.04.18.
 */
@Aspect
@Service
public class EsbMessageIdInterceptor {

    @Around("execution(public * *(..)) "
            + "&& within(com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener)")
    public Object markThreadName(ProceedingJoinPoint joinPoint) throws Throwable {
        String name = Arrays.stream(joinPoint.getArgs())
                .filter(it -> it instanceof BaseMessage)
                .findFirst()
                .map(it -> (BaseMessage) it)
                .map(it -> String.format("%s %s", it.getId(), it.getClass().getSimpleName()))
                .orElse(Thread.currentThread().getName());

        String oldName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(name);
            return joinPoint.proceed();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}
