package com.gtc.opportunity.trader.service.stat;

import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.stat.MainKey;
import com.gtc.opportunity.trader.domain.stat.StatId;
import com.gtc.opportunity.trader.domain.stat.general.BaseXoTradeStat;
import com.gtc.opportunity.trader.domain.stat.general.Snapshot;
import com.gtc.opportunity.trader.domain.stat.general.XoTradeStatDaily;
import com.gtc.opportunity.trader.domain.stat.general.XoTradeStatTotal;
import com.gtc.opportunity.trader.repository.stat.general.BaseXoTradeStatRepository;
import com.gtc.opportunity.trader.repository.stat.general.XoTradeStatDailyRepository;
import com.gtc.opportunity.trader.repository.stat.general.XoTradeStatTotalRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.gtc.opportunity.trader.config.Const.Common.XO_OPPORTUNITY_PREFIX;
import static com.gtc.opportunity.trader.config.Const.Scheduled.PUSH_STAT;

/**
 * Created by Valentyn Berezin on 26.02.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XoStatService {

    private final XoTradeStatDailyRepository dailyRepository;
    private final XoTradeStatTotalRepository totalRepository;
    private final CurrentTimestamp currentTimestamp;
    private final KeyExtractor keyExtractor;

    // StatId is partial - lacks sinceDate
    private final Map<MainKey, Snapshot> snapshots = new ConcurrentHashMap<>();

    public void ackClosedOpportunity(CrossMarketOpportunity opportunity) {
        snapshots.compute(keyExtractor.extractKeyOmitDate(opportunity), (key, value) -> {
            Snapshot snapshot = null == value ? new Snapshot() : value;
            map(snapshot, opportunity);
            snapshot.setRecordCount(snapshot.getRecordCount() + 1);
            return snapshot;
        });
    }

    @Transactional
    @Scheduled(fixedRateString = PUSH_STAT)
    public void pushStats() {
        LocalDateTime now = currentTimestamp.dbNow();
        LocalDate today = currentTimestamp.dbNow().toLocalDate();
        snapshots.forEach((key, value) -> {
                computeAndPersist(key, value, today, now, build(XoTradeStatDaily::new), dailyRepository);
                computeAndPersist(key, value, LocalDate.ofEpochDay(0), now,
                        build(XoTradeStatTotal::new), totalRepository);
        });
        snapshots.clear();
    }

    private static <T extends BaseXoTradeStat> Function<StatId, T> build(Supplier<T> create) {
        return key -> {
            T value = create.get();
            value.setSnapshot(new Snapshot());
            value.setId(key);
            return value;
        };
    }

    private static <T extends BaseXoTradeStat> void computeAndPersist(
            MainKey key, Snapshot snapshot, LocalDate today, LocalDateTime now,
            Function<StatId, T> build, BaseXoTradeStatRepository<T> repo) {
        // key lacks sinceDate - inject it into 'new' key
        StatId fullId = new StatId(key, today);
        T ent = repo.findById(fullId).orElseGet(() -> build.apply(fullId));
        ent.setSnapshot(merge(ent.getSnapshot(), snapshot));
        ent.setUpdatedAt(now);
        repo.save(ent);
    }

    private static void map(Snapshot snapshot, CrossMarketOpportunity opportunity) {
        snapshot.setMinSellAmount(
                Math.min(snapshot.getMinSellAmount(), opportunity.getMarketToBestSellAmount().getMin()));
        snapshot.setTotSellAmount(snapshot.getTotSellAmount().add(
                BigDecimal.valueOf(opportunity.getMarketToBestSellAmount().getTotal() / opportunity.getEventCount())));
        snapshot.setMaxSellAmount(
                Math.max(snapshot.getMaxSellAmount(), opportunity.getMarketToBestSellAmount().getMax()));

        snapshot.setMinBuyAmount(
                Math.min(snapshot.getMinBuyAmount(), opportunity.getMarketFromBestBuyAmount().getMin()));
        snapshot.setTotBuyAmount(snapshot.getTotBuyAmount().add(
                BigDecimal.valueOf(opportunity.getMarketFromBestBuyAmount().getTotal() / opportunity.getEventCount())));
        snapshot.setMaxBuyAmount(
                Math.max(snapshot.getMaxBuyAmount(), opportunity.getMarketFromBestBuyAmount().getMax()));

        snapshot.setMinHistWin(Math.min(snapshot.getMinHistWin(), opportunity.getHistWin().getMin()));
        snapshot.setTotHistWin(snapshot.getTotHistWin().add(
                BigDecimal.valueOf(opportunity.getHistWin().getTotal() / opportunity.getEventCount())));
        snapshot.setMaxHistWin(Math.max(snapshot.getMaxHistWin(), opportunity.getHistWin().getMax()));

        double dur = ChronoUnit.MILLIS.between(opportunity.getOpenedOn(), opportunity.getClosedOn()) / 1000.0;
        snapshot.setMinDurationS(Math.min(snapshot.getMinDurationS(), dur));
        snapshot.setTotDurationS(snapshot.getTotDurationS().add(BigDecimal.valueOf(dur)));
        snapshot.setMaxDurationS(Math.max(snapshot.getMaxDurationS(), dur));
        snapshot.setLastOpportunityId(XO_OPPORTUNITY_PREFIX + opportunity.getUuid());
        snapshot.setLastOpportCreatedOn(null == snapshot.getLastOpportCreatedOn() ? opportunity.getOpenedOn()
                : snapshot.getLastOpportCreatedOn());
        double sep = ChronoUnit.MILLIS.between(snapshot.getLastOpportCreatedOn(), opportunity.getOpenedOn()) / 1000.0;
        snapshot.setMaxSeparationS(Math.max(sep, snapshot.getMaxSeparationS()));
        snapshot.setLastOpportCreatedOn(opportunity.getOpenedOn());
    }

    private static Snapshot merge(Snapshot older, Snapshot newer) {

        Snapshot snapshot = new Snapshot();

        snapshot.setMinSellAmount(Math.min(older.getMinSellAmount(), newer.getMinSellAmount()));
        snapshot.setTotSellAmount(older.getTotSellAmount().add(newer.getTotSellAmount()));
        snapshot.setMaxSellAmount(Math.max(older.getMaxSellAmount(), newer.getMaxSellAmount()));

        snapshot.setMinBuyAmount(Math.min(older.getMinBuyAmount(), newer.getMinBuyAmount()));
        snapshot.setTotBuyAmount(older.getTotBuyAmount().add(newer.getTotBuyAmount()));
        snapshot.setMaxBuyAmount(Math.max(older.getMaxBuyAmount(), newer.getMaxBuyAmount()));

        snapshot.setMinHistWin(Math.min(older.getMinHistWin(), newer.getMinHistWin()));
        snapshot.setTotHistWin(older.getTotHistWin().add(newer.getTotHistWin()));
        snapshot.setMaxHistWin(Math.max(older.getMaxHistWin(), newer.getMaxHistWin()));

        snapshot.setMaxTickerWin(Math.max(older.getMaxTickerWin(), newer.getMaxTickerWin()));

        snapshot.setMinDurationS(Math.min(older.getMinDurationS(), newer.getMinDurationS()));
        snapshot.setTotDurationS(older.getTotDurationS().add(newer.getTotDurationS()));
        snapshot.setMaxDurationS(Math.max(older.getMaxDurationS(), newer.getMaxDurationS()));

        snapshot.setRecordCount(older.getRecordCount() + newer.getRecordCount());

        snapshot.setLastOpportunityId(newer.getLastOpportunityId());
        snapshot.setLastOpportCreatedOn(newer.getLastOpportCreatedOn());
        snapshot.setMaxSeparationS(Math.max(older.getMaxSeparationS(), newer.getMaxSeparationS()));

        return snapshot;
    }
}
