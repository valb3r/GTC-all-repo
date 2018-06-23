package com.gtc.opportunity.trader.service.opportunity.creation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.config.CacheConfig;
import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.repository.ClientConfigRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason.LOW_PROFIT_PRE;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Slf4j
@Service
public class OpportunityAnalyzer {

    private final ClientConfigRepository cfgRepository;
    private final OpportunitySatisfierService satisfierService;
    private final OpportunityMapperFactory mapper;

    private final Cache<String, Optional<ClientConfig>> cfgCache;

    public OpportunityAnalyzer(ClientConfigRepository cfgRepository, OpportunitySatisfierService satisfierService,
                               CacheConfig config, OpportunityMapperFactory mapper) {
        this.cfgRepository = cfgRepository;
        this.satisfierService = satisfierService;

        this.cfgCache = CacheBuilder.newBuilder()
                .maximumSize(config.getCfgCache().getSize())
                .expireAfterWrite(config.getCfgCache().getLiveS(), TimeUnit.SECONDS)
                .build();
        this.mapper = mapper;
    }

    @Transactional
    public void newOpportunity(FullCrossMarketOpportunity opportunity) {
        Optional<ClientConfig> fromOpt = getCfg(opportunity.getClientFrom(), opportunity.getCurrencyFrom(),
                opportunity.getCurrencyTo());
        Optional<ClientConfig> toOpt = getCfg(opportunity.getClientTo(), opportunity.getCurrencyFrom(),
                opportunity.getCurrencyTo());

        Supplier<RejectionException> noCfg = () -> new RejectionException(Reason.NO_CONFIG);

        if (!fromOpt.map(it -> it.getClient().isEnabled() && it.isEnabled()).orElseThrow(noCfg)
                || !toOpt.map(it -> it.getClient().isEnabled() && it.isEnabled()).orElseThrow(noCfg)) {
            throw new RejectionException(Reason.DISABLED);
        }

        ClientConfig from = fromOpt.get();
        ClientConfig to = toOpt.get();

        OpportunityMapperFactory.MappedOpp opp = mapper.map(opportunity, from, to);

        Checker.validateAtLeast(LOW_PROFIT_PRE, opp.profitPct(), from.getMinProfitabilityPct().doubleValue());
        Checker.validateAtLeast(LOW_PROFIT_PRE, opp.profitPct(), to.getMinProfitabilityPct().doubleValue());

        satisfierService.satisfyOpportunity(from, to, opportunity);
    }

    @Transactional
    public void cancelledOpportunity(CrossMarketOpportunity opportunity) {
        // TODO: Implement in-flight cancellation
    }

    @SneakyThrows
    private Optional<ClientConfig> getCfg(String clientName, TradingCurrency from, TradingCurrency to) {
        return cfgCache.get(
                clientName + from.toString() + to.toString(),
                () -> cfgRepository.findActiveByKey(clientName, from, to)
        );
    }
}
