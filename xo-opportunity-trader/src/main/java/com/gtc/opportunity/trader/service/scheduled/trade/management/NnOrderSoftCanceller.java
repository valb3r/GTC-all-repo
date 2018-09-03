package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Service
@RequiredArgsConstructor
public class NnOrderSoftCanceller {

    private final WsGatewayCommander commander;
    private final TradeRepository tradeRepository;
    private final CurrentTimestamp dbTime;
    private final NnConfigRepository nnCfg;
    private final StateMachineService<TradeStatus, TradeEvent> tradeMachines;

    /*@Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "#{${app.schedule.order.cancellerS} * 1000}")
    public void softCancel() {
        LocalDateTime now = dbTime.dbNow();
        Map<NnOrderHardCanceller.Key, NnConfig> byClient = getActiveConfigs();

        cancelOldMasters(now, byClient);
        cancelOldSlaves(now, byClient);
    }*/
}
