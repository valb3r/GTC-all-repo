package com.gtc.tradinggateway.aspect.ws;

import com.gtc.tradinggateway.service.BaseWsClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Created by Valentyn Berezin on 20.02.18.
 */
@Aspect
@Component
public class WsReadyAspect {

    @Around("execution(public * *(..)) && !execution(public String name()) && @within(ann) "
            + "&& !@annotation(com.gtc.tradinggateway.aspect.ws.IgnoreWsReady)")
    public Object checkIfReady(ProceedingJoinPoint joinPoint, WsReady ann) throws Throwable {
        if (!(joinPoint.getThis() instanceof BaseWsClient)) {
            return joinPoint.proceed();
        }

        BaseWsClient client = (BaseWsClient) joinPoint.getThis();

        if (!client.isReady()) {
            throw new IllegalStateException(String.format(
                    "Client %s not ready, is connected: %s",
                    client.name(),
                    !client.isDisconnected())
            );
        }

        return joinPoint.proceed();
    }
}
