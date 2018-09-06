package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.opportunity.trader.BaseNnTradeInitialized;
import com.gtc.opportunity.trader.domain.SoftCancelConfig;
import com.gtc.opportunity.trader.repository.SoftCancelConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 04.09.18.
 */
public class NnOrderSoftCancellerIT extends BaseNnTradeInitialized {

    @Autowired
    private NnOrderSoftCanceller canceller;

    @Autowired
    private SoftCancelConfigRepository cancelConfigRepository;

    @Autowired
    private TransactionTemplate template;

    @BeforeTransaction
    public void initializeDb() {
        // FIXME: Without this wrapping  and finding client entity again we would get detached entity exception
        template.execute(call -> {
            SoftCancelConfig cc = SoftCancelConfig.builder()
                    .clientCfg(configRepository.findById(createdConfig.getId()).get())
                    .doneToCancelRatio(BigDecimal.TEN)
                    .minPriceLossPct(BigDecimal.ONE)
                    .maxPriceLossPct(new BigDecimal("2"))
                    .waitM(30)
                    .enabled(true)
                    .build();
            cancelConfigRepository.save(cc);
            return null;
        });

    }

    @Test
    void isCreated() {
        canceller.softCancel();
    }

    @Test
    void usesMinLoss() {

    }

    @Test
    void usesMaxLoss() {

    }

    @Test
    void usesDoneToCancelRatio() {

    }

    @Test
    void validatesWalletBalance() {

    }
}
