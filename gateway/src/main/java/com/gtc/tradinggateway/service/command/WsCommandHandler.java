package com.gtc.tradinggateway.service.command;

import com.google.common.base.Throwables;
import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.WithOrderId;
import com.gtc.model.gateway.command.account.GetAllBalancesCommand;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import com.gtc.model.gateway.command.withdraw.WithdrawCommand;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.response.ErrorResponse;
import com.gtc.model.gateway.response.account.GetAllBalancesResponse;
import com.gtc.model.gateway.response.create.CreateOrderResponse;
import com.gtc.model.gateway.response.manage.CancelOrderResponse;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.model.gateway.response.manage.ListOpenOrdersResponse;
import com.gtc.model.gateway.response.withdraw.WithdrawOrderResponse;
import com.gtc.tradinggateway.aspect.rate.RateTooHighException;
import com.gtc.tradinggateway.config.ClientsConf;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.*;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.socket.WebSocketSession;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 20.02.18.
 */
@Slf4j
@Service
@Async
public class WsCommandHandler {

    private final SubsRegistry registry;

    private final Map<String, Account> accountOps;
    private final Map<String, CreateOrder> createOps;
    private final Map<String, ManageOrders> manageOps;
    private final Map<String, Withdraw> withdrawOps;

    public WsCommandHandler(ClientsConf conf, SubsRegistry registry, List<Account> accountCmds,
                            List<CreateOrder> createCmds, List<ManageOrders> manageCmds,
                            List<Withdraw> withdrawCmds) {
        this.registry = registry;
        accountOps = accountCmds.stream().filter(it -> conf.getActive().contains(it.name()))
                .collect(Collectors.toMap(ClientNamed::name, it -> it));
        createOps = createCmds.stream().filter(it -> conf.getActive().contains(it.name()))
                .collect(Collectors.toMap(ClientNamed::name, it -> it));
        manageOps = manageCmds.stream().filter(it -> conf.getActive().contains(it.name()))
                .collect(Collectors.toMap(ClientNamed::name, it -> it));
        withdrawOps = withdrawCmds.stream().filter(it -> conf.getActive().contains(it.name()))
                .collect(Collectors.toMap(ClientNamed::name, it -> it));
    }

    @Trace(dispatcher = true)
    public void getAllBalances(WebSocketSession session, @Valid GetAllBalancesCommand command) {
        log.info("Request to get balances {}", command);
        doExecute(session, command, accountOps, (handler, cmd) -> {
            Map<TradingCurrency, BigDecimal> balances = handler.balances();

            log.info("Got balances {} for {} of {}", balances, cmd.getId(), cmd.getClientName());
            return GetAllBalancesResponse.builder()
                    .clientName(cmd.getClientName())
                    .id(cmd.getId())
                    .balances(balances.entrySet().stream()
                            .collect(Collectors.toMap(it -> it.getKey().getCode(), Map.Entry::getValue))
                    ).build();
        });
    }

    @Trace(dispatcher = true)
    public void create(WebSocketSession session, @Valid CreateOrderCommand command) {
        log.info("Request to create order {}", command);
        doExecute(session, command, createOps, (handler, cmd) -> {
            Optional<OrderCreatedDto> res = handler.create(
                    cmd.getOrderId(),
                    TradingCurrency.fromCode(cmd.getCurrencyFrom()),
                    TradingCurrency.fromCode(cmd.getCurrencyTo()),
                    cmd.getAmount(),
                    cmd.getPrice()
            );

            log.info("Created {} for {} of {}", res, cmd.getId(), cmd.getClientName());
            return res.map(id -> CreateOrderResponse.builder()
                    .clientName(cmd.getClientName())
                    .id(cmd.getId())
                    .requestOrderId(cmd.getId())
                    .orderId(id.getAssignedId())
                    .isExecuted(id.isExecuted())
                    .build()
            ).orElse(null);
        });
    }

    @Trace(dispatcher = true)
    public void create(WebSocketSession session, @Valid MultiOrderCreateCommand command) {
        log.info("Request to create multi-orders {}", command);
        command.getCommands().stream().map(CreateOrderCommand::getClientName).forEach(name -> {
            if (!createOps.keySet().contains(name)) {
                throw new NoClientException(name);
            }

            checkReadiness(name, createOps.get(name));
        });

        // FIXME - far from ideal this 'type' mapping should be redesigned
        command.getCommands().parallelStream().forEach(it -> {
            it.setType(it.type());
            create(session, it);
        });
    }

    @Trace(dispatcher = true)
    public void get(WebSocketSession session, @Valid GetOrderCommand command) {
        log.info("Request to get order {}", command);
        doExecute(session, command, manageOps, (handler, cmd) -> {
            OrderDto res = handler.get(
                    cmd.getOrderId()
            ).orElseThrow(() -> new NotFoundException(cmd.getOrderId()));

            log.info("Found {} for {} of {}", res, cmd.getOrderId(), cmd.getClientName());
            return GetOrderResponse.builder()
                    .clientName(cmd.getClientName())
                    .id(cmd.getId())
                    .order(res)
                    .build();
        });
    }

    @Trace(dispatcher = true)
    public void listOpen(WebSocketSession session, @Valid ListOpenCommand command) {
        log.info("Request to list orders {}", command);
        doExecute(session, command, manageOps, (handler, cmd) -> {
            List<OrderDto> res = handler.getOpen(
                    TradingCurrency.fromCode(cmd.getCurrencyFrom()),
                    TradingCurrency.fromCode(cmd.getCurrencyTo())
            );

            log.info("Found open orders {} for {}", res, cmd.getClientName());
            return ListOpenOrdersResponse.builder()
                    .clientName(cmd.getClientName())
                    .id(cmd.getId())
                    .orders(res)
                    .build();
        });
    }

    @Trace(dispatcher = true)
    public void cancel(WebSocketSession session, @Valid CancelOrderCommand command) {
        log.info("Request to cancel order {}", command);
        doExecute(session, command, manageOps, (handler, cmd) -> {
            handler.cancel(cmd.getOrderId());

            log.info("Cancelled order {} for {}", cmd.getOrderId(), cmd.getClientName());
            return CancelOrderResponse.builder()
                    .clientName(cmd.getClientName())
                    .id(cmd.getId())
                    .orderId(command.getOrderId())
                    .build();
        });
    }

    @Trace(dispatcher = true)
    public void withdraw(WebSocketSession session, @Valid WithdrawCommand command) {
        log.info("Request to withdraw {}", command);
        doExecute(session, command, withdrawOps, (handler, cmd) -> {
            handler.withdraw(
                    TradingCurrency.fromCode(cmd.getCurrency()),
                    cmd.getAmount(),
                    cmd.getToDestination()
            );

            log.info("Withdraw {} by {} to {}", cmd.getCurrency(), cmd.getAmount(), cmd.getToDestination());
            return WithdrawOrderResponse.builder()
                    .clientName(cmd.getClientName())
                    .id(cmd.getId())
                    .currency(cmd.getCurrency())
                    .amount(cmd.getAmount())
                    .toDestination(cmd.getToDestination())
                    .build();
        });
    }

    private void checkReadiness(String name, CreateOrder createOrderExec) {
        if (!(createOrderExec instanceof BaseWsClient)) {
            return;
        }

        BaseWsClient wsClient = (BaseWsClient) createOrderExec;
        if (!wsClient.isReady()) {
            throw new NotReadyException(name);
        }
    }

    private <T extends ClientNamed, U extends BaseMessage> void doExecute(
            WebSocketSession session,
            U message,
            Map<String, T> handlers,
            BiFunction<T, U, ? extends BaseMessage> executor) {
        T handler = handlers.get(message.getClientName());

        if (null == handler) {
            log.warn("Missing handler for {}", message);
            ErrorResponse error = buildError(message, new NoClientException());
            registry.doSend(session, error);
            return;
        }

        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(message.getClientName() + " / " + message.getId());

        RetryTemplate retryTemplate = retryTemplate(message);
        try {
            BaseMessage result = retryTemplate.execute(context -> executor.apply(handler, message));
            // result can be null if it was WS based request
            if (null != result) {
                registry.doSend(session, result);
            }
        } catch (RateTooHighException ex) {
            NewRelic.noticeError(ex);
            ErrorResponse error = buildError(message, ex);
            error.setTransient(true);
            log.error("Sending transient error message {} in response to {}", error, message.getId());
            registry.doSend(session, error);
        } catch (Exception ex) {
            NewRelic.noticeError(ex);
            ErrorResponse error = buildError(message, ex);
            log.error("Sending error message {} in response to {}", error, message.getId());
            registry.doSend(session, error);
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    private <U extends BaseMessage> RetryTemplate retryTemplate(U message) {
        RetryTemplate template = new RetryTemplate();

        if (null != message.getRetryStrategy()) {
            template.setRetryPolicy(new SimpleRetryPolicy(message.getRetryStrategy().getMaxRetries()));
            ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
            backoff.setInitialInterval(message.getRetryStrategy().getBaseDelayMs());
            backoff.setMultiplier(message.getRetryStrategy().getBackOffMultiplier());
            template.setBackOffPolicy(backoff);
            template.registerListener(new DoRetryListener());
        } else {
            template.setRetryPolicy(new NeverRetryPolicy());
        }

        return template;
    }

    private static ErrorResponse buildError(BaseMessage origin, Throwable forExc) {
        ErrorResponse resp = new ErrorResponse();
        resp.setClientName(origin.getClientName());
        resp.setId(UUID.randomUUID().toString());
        resp.setOnMessageId(origin.getId());
        resp.setOccurredOn(origin.toString());
        resp.setOccurredOnType(origin.getType());
        String rootCause = Throwables.getStackTraceAsString(Throwables.getRootCause(forExc));
        if (forExc instanceof HttpStatusCodeException) {
            HttpStatusCodeException codeEx = (HttpStatusCodeException) forExc;
            rootCause = String.format("Http status %d  / (%s) / (%s) / %s",
                    codeEx.getRawStatusCode(),
                    codeEx.getResponseBodyAsString(),
                    codeEx.getMessage(),
                    rootCause);
        }

        resp.setErrorCause(rootCause);

        if (origin instanceof WithOrderId) {
            resp.setOrderId(((WithOrderId) origin).getOrderId());
        }

        return resp;
    }

    private static class NoClientException extends IllegalStateException {

        NoClientException() {
        }

        NoClientException(String s) {
            super(s);
        }
    }

    private static class NotFoundException extends IllegalArgumentException {

        NotFoundException(String s) {
            super(s);
        }
    }

    private static class NotReadyException extends IllegalArgumentException {

        NotReadyException(String s) {
            super(s);
        }
    }

    @Slf4j
    private static class DoRetryListener implements RetryListener {

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                                                   Throwable throwable) {
            // NOOP
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                     Throwable throwable) {
            log.info("Request failed, will retry if strategy is available", throwable);
            NewRelic.noticeError(throwable);
        }
    }
}
