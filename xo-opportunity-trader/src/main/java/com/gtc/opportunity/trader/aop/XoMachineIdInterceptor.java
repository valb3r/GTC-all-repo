package com.gtc.opportunity.trader.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Created by Valentyn Berezin on 06.04.18.
 */
@Aspect
@Service
public class XoMachineIdInterceptor {

    @Around("execution(public * *(..)) "
            + "&& within(com.gtc.opportunity.trader.service.statemachine.xoaccept.XoAcceptMachine)")
    public Object markThreadName(ProceedingJoinPoint joinPoint) throws Throwable {
        String name = Arrays.stream(joinPoint.getArgs())
                .filter(it -> it instanceof StateContext)
                .findFirst()
                .map(it -> (StateContext) it)
                .map(it -> String.format("%s %s %s",
                        it.getStateMachine().getId(),
                        it.getMessage().getHeaders().getId(),
                        it.getTarget())
                ).orElse(Thread.currentThread().getName());

        String oldName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(name);
            return joinPoint.proceed();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}
