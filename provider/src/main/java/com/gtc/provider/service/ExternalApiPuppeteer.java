package com.gtc.provider.service;

import com.gtc.provider.clients.WsClient;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.CONF_ROOT_SCHEDULE_CHILD;

/**
 * Initializes all ws to 3rd parties connections and maintains them up.
 * We may need to set cloudflare clearance cookies:
 * "Cookie" = "cf_clearance=....",
 * "User-Agent" = "..."
 */
@Slf4j
@Service
public class ExternalApiPuppeteer {

    // should be used in case cookies are needed
    private final Map<String, Map<String, String>> clientHeaders = new ConcurrentHashMap<>();
    private final DeclaredClientsProvider clients;

    public ExternalApiPuppeteer(DeclaredClientsProvider clients) {
        this.clients = clients;
    }

    @Scheduled(fixedDelayString = "#{${" + CONF_ROOT_SCHEDULE_CHILD + "puppeteerS} * 1000}")
    public void connection() {
        try {
            clients.getClientList().stream()
                    .filter(WsClient::isDisconnected)
                    .forEach(client -> client.connect(getHeaders(client.name())));
        } catch (RuntimeException ex) {
            log.error("Failed connecting", ex);
        }
    }

    private Map<String, String> getHeaders(String name) {
        Map<String, String> headers = clientHeaders.get(name);
        if (null == headers) {
            return ImmutableMap.of();
        }

        return headers;
    }
}
