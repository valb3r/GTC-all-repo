package com.gtc.tradinggateway.service;

import com.gtc.tradinggateway.config.ClientsConf;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.gtc.tradinggateway.config.Const.Schedule.CONF_ROOT_SCHEDULE_CHILD;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Slf4j
@Service
public class WsApiPuppeteer {

    // should be used in case cookies are needed
    private final List<? extends BaseWsClient> wsClients;

    public WsApiPuppeteer(List<? extends BaseWsClient> wsClients, ClientsConf conf) {
        this.wsClients = wsClients.stream()
                .filter(it -> conf.getActive().contains(it.name()))
                .collect(Collectors.toList());
    }

    @Trace(dispatcher = true)
    @Scheduled(fixedDelayString = "#{${" + CONF_ROOT_SCHEDULE_CHILD + "puppeteerS} * 1000}")
    public void connection() {
        try {
            wsClients.stream()
                    .filter(BaseWsClient::isDisconnected)
                    .forEach(BaseWsClient::connect);
        } catch (RuntimeException ex) {
            log.error("Failed connecting", ex);
            NewRelic.noticeError(ex);
        }
    }
}
