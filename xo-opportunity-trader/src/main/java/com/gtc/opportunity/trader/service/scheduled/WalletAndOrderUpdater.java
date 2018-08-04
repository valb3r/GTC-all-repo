package com.gtc.opportunity.trader.service.scheduled;

import com.google.common.collect.ImmutableSet;
import com.gtc.model.gateway.RetryStrategy;
import com.gtc.model.gateway.command.account.GetAllBalancesCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.ClientRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.dto.ByClientAndPair;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.gtc.opportunity.trader.domain.TradeStatus.OPENED;
import static com.gtc.opportunity.trader.domain.TradeStatus.UNKNOWN;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAndOrderUpdater {

    private static final String WALLET_UPDATE_MS = "#{${app.schedule.wallet.updateS} * 1000}";
    private static final String BULK_UPDATE_STATUS_MS = "#{${app.schedule.order.bulkUpdateStatusS} * 1000}";
    private static final String STUCK_UPDATE_STATUS_MS = "#{${app.schedule.order.stuckUpdateStatusS} * 1000}";
    private static final String TIMED_OUT_STATUS_MS = "#{${app.schedule.order.timedOutCheckS} * 1000}";

    private static final Set<TradeStatus> UPDATE_ELIGIBLE = ImmutableSet.of(UNKNOWN, OPENED);

    private final ClientRepository clientRepository;
    private final TradeRepository tradeRepository;
    private final CurrentTimestamp currentTimestamp;
    private final WsGatewayCommander commander;
    private final StateMachineService<TradeStatus, TradeEvent> stateMachineService;

    @Value("${app.updater.maxToCheckStuckPerClient}")
    private int maxToCheckStuckPerClient;

    @Value("${app.updater.orderTimeoutS}")
    private int orderTimeoutS;

    @Value(BULK_UPDATE_STATUS_MS)
    private int bulkUpdateMS;

    @Value(STUCK_UPDATE_STATUS_MS)
    private int stuckUpdateMS;

    @Trace(dispatcher = true)
    @Scheduled(fixedRateString = BULK_UPDATE_STATUS_MS)
    @Transactional(readOnly = true)
    public void bulkUpdateOrderStatus() {
        LocalDateTime before = currentTimestamp.dbNow().minus(bulkUpdateMS, ChronoUnit.MILLIS);
        List<ByClientAndPair> symbols = new ArrayList<>(
                tradeRepository.findSymbolsWithActiveOrders(UPDATE_ELIGIBLE, before, true)
        );

        // shuffle so that in case of rate-limiting we get a change to get response
        Collections.shuffle(symbols);

        symbols.stream()
                .map(sym -> ListOpenCommand.builder()
                        .clientName(sym.getClient().getName())
                        .currencyFrom(sym.getFrom().getCode())
                        .currencyTo(sym.getTo().getCode())
                        .id(UuidGenerator.get())
                        .build()
                ).forEach(commander::listOpenOrders);
    }

    // tries to update selected orders which were stuck some time ago
    @Trace(dispatcher = true)
    @Scheduled(initialDelayString = STUCK_UPDATE_STATUS_MS, fixedRateString = STUCK_UPDATE_STATUS_MS)
    @Transactional(readOnly = true)
    public void stuckUpdateOrderStatus() {
        LocalDateTime before = currentTimestamp.dbNow().minus(stuckUpdateMS, ChronoUnit.MILLIS);
        tradeRepository.findByStatusInAndStatusUpdatedBefore(UPDATE_ELIGIBLE, before, true).stream()
                .collect(Collectors.groupingBy(it -> it.getClient().getName()))
                .forEach((client, orders) -> {
                    // shuffle so that in case of rate-limiting we get a change to get response
                    Collections.shuffle(orders);
                    orders.stream().limit(maxToCheckStuckPerClient).map(
                            td -> {
                                GetOrderCommand command = GetOrderCommand.builder()
                                        .id(UuidGenerator.get())
                                        .clientName(td.getClient().getName())
                                        .orderId(td.getAssignedId())
                                        .build();
                                command.setRetryStrategy(RetryStrategy.BASIC_RETRY);
                                return command;
                            }
                    ).forEach(commander::getOrder);
                });
    }

    @Trace(dispatcher = true)
    @Scheduled(initialDelayString = TIMED_OUT_STATUS_MS, fixedRateString = TIMED_OUT_STATUS_MS)
    @Transactional // no (readOnly = true) since parent transaction will mandate RO
    public void orderTimeouter() {
        LocalDateTime before = currentTimestamp.dbNow().minusSeconds(orderTimeoutS);
        tradeRepository
                .findByStatusInAndStatusUpdatedBefore(ImmutableSet.of(TradeStatus.UNKNOWN), before, true)
                .forEach(it -> stateMachineService.acquireStateMachine(it.getId()).sendEvent(TradeEvent.TIMEOUT));
    }

    @Trace(dispatcher = true)
    @Scheduled(initialDelayString = WALLET_UPDATE_MS, fixedRateString = WALLET_UPDATE_MS)
    @Transactional(readOnly = true)
    public void walletUpdater() {
        clientRepository.findByEnabledTrue().stream()
                .map(it -> GetAllBalancesCommand.builder()
                        .id(UuidGenerator.get())
                        .clientName(it.getName())
                        .build())
                .forEach(commander::getBalances);
    }
}
