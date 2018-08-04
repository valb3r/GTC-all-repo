package com.gtc.opportunity.trader.service.stat.xo;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.stat.MainKey;
import com.gtc.opportunity.trader.domain.stat.RejectedId;
import com.gtc.opportunity.trader.domain.stat.StatId;
import com.gtc.opportunity.trader.domain.stat.rejected.BaseXoTradeRejectedStat;
import com.gtc.opportunity.trader.domain.stat.rejected.RejectionStat;
import com.gtc.opportunity.trader.domain.stat.rejected.XoTradeRejectedStatDaily;
import com.gtc.opportunity.trader.domain.stat.rejected.XoTradeRejectedStatTotal;
import com.gtc.opportunity.trader.repository.stat.rejected.BaseXoTradeRejectedStatRepository;
import com.gtc.opportunity.trader.repository.stat.rejected.XoTradeRejectedStatDailyRepository;
import com.gtc.opportunity.trader.repository.stat.rejected.XoTradeRejectedStatTotalRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.stat.KeyExtractor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Created by Valentyn Berezin on 27.03.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RejectedTradeStatService {

    private final XoTradeRejectedStatDailyRepository dailyRepository;
    private final XoTradeRejectedStatTotalRepository totalRepository;
    private final CurrentTimestamp currentTimestamp;
    private final KeyExtractor keyExtractor;

    private final Map<Key, RejectionStat> snapshots = new ConcurrentHashMap<>();

    public void ackRejection(RejectionException ex, FullCrossMarketOpportunity opp) {

        snapshots.compute(key(ex, opp), (key, value) -> {
            RejectionStat snapshot = null == value ? new RejectionStat() : value;
            map(snapshot, ex, opp);
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
            computeAndPersist(key, value, today, now, build(XoTradeRejectedStatDaily::new),
                    dailyRepository);
            computeAndPersist(key, value, LocalDate.ofEpochDay(0), now,
                    build(XoTradeRejectedStatTotal::new), totalRepository);
        });
        snapshots.clear();
    }

    private static <T extends BaseXoTradeRejectedStat> Function<RejectedId, T> build(Supplier<T> create) {
        return key -> {
            T value = create.get();
            value.setRejections(new RejectionStat());
            value.setId(key);
            return value;
        };
    }

    private static <T extends BaseXoTradeRejectedStat> void computeAndPersist(
            Key key, RejectionStat reason, LocalDate today, LocalDateTime now,
            Function<RejectedId, T> build, BaseXoTradeRejectedStatRepository<T> repo) {
        // key lacks sinceDate - inject it into 'new' key
        RejectedId fullId = new RejectedId(new StatId(key.getMainKey(), today), key.getReason().getMsg());
        T ent = repo.findById(fullId).orElseGet(() -> build.apply(fullId));
        ent.setRejections(merge(ent.getRejections(), reason));
        ent.setUpdatedAt(now);
        repo.save(ent);
    }

    private Key key(RejectionException ex, FullCrossMarketOpportunity opp) {
        return new Key(
                keyExtractor.extractKeyOmitDate(opp.getOpportunity()),
                ex.getReason()
        );
    }

    private void map(RejectionStat stat, RejectionException ex, FullCrossMarketOpportunity opp) {
        stat.setLastValue(ex.getValue());
        stat.setTotalValue(add(stat.getTotalValue(), ex.getValue()));
        stat.setLastThreshold(ex.getThreshold());
        stat.setTotalThreshold(add(stat.getTotalThreshold(), ex.getThreshold()));
        stat.setRecordCount(stat.getRecordCount() + 1);
        stat.setLastOpportunityId(XO_OPPORTUNITY_PREFIX + opp.getUuid());
        stat.setLastOpportCreatedOn(null == stat.getLastOpportCreatedOn() ? opp.getOpenedOn()
                : stat.getLastOpportCreatedOn());
        double sep = ChronoUnit.MILLIS.between(stat.getLastOpportCreatedOn(), opp.getOpenedOn()) / 1000.0;
        stat.setMaxSeparationS(Math.max(sep, stat.getMaxSeparationS()));
        stat.setLastOpportCreatedOn(opp.getOpenedOn());
    }

    private static RejectionStat merge(RejectionStat older, RejectionStat newer) {
        older.setLastValue(newer.getLastValue());
        older.setTotalValue(add(older.getTotalValue(), newer.getLastValue()));
        older.setLastThreshold(newer.getLastThreshold());
        older.setTotalThreshold(add(older.getTotalThreshold(), newer.getLastThreshold()));
        older.setRecordCount(older.getRecordCount() + 1);
        older.setLastOpportunityId(newer.getLastOpportunityId());
        older.setLastOpportCreatedOn(newer.getLastOpportCreatedOn());
        older.setMaxSeparationS(Math.max(older.getMaxSeparationS(), newer.getMaxSeparationS()));
        return older;
    }

    private static Double add(Double one, Double two) {
        if (one == null && two == null) {
            return null;
        }

        return nvl(one) + nvl(two);
    }

    private static double nvl(Double value) {
        return null == value ? 0 : value;
    }

    @Getter
    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static final class Key {

        private final MainKey mainKey;
        private final Reason reason;
    }
}
