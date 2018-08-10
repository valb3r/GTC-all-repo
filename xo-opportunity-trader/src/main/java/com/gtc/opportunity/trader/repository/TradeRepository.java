package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.dto.ByClientAndPair;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
public interface TradeRepository extends CrudRepository<Trade, String> {

    Optional<Trade> findById(String id);

    Collection<Trade> findByXoOrderId(int orderId);

    @Query("SELECT t FROM Trade t WHERE t.client.name = :#{#id.clientName} AND t.assignedId = :#{#id.assignedId}")
    Optional<Trade> findByAssignedId(@Param("id") Trade.EsbKey id);

    @Query("SELECT new com.gtc.opportunity.trader.repository.dto.ByClientAndPair(t.client, t.currencyFrom, t.currencyTo) " +
            "FROM Trade t " +
            "WHERE t.status IN (:statuses) AND t.statusUpdated <= :lastUpdate " +
            "AND t.client.enabled = :clientEnabled " +
            "GROUP BY t.client, t.currencyFrom, t.currencyTo")
    Set<ByClientAndPair> findSymbols(
            @Param("statuses") Collection<TradeStatus> statuses,
            @Param("lastUpdate") LocalDateTime lastUpdate,
            @Param("clientEnabled") boolean clientEnabled);

    @Query("SELECT t FROM Trade t WHERE t.status IN (:statuses) " +
            "AND t.statusUpdated <= :lastUpdate " +
            "AND t.client.enabled = :clientEnabled " +
            "ORDER BY t.statusUpdated DESC")
    List<Trade> findByStatusInAndStatusUpdatedBefore(
            @Param("statuses") Collection<TradeStatus> statuses,
            @Param("lastUpdate") LocalDateTime lastUpdate,
            @Param("clientEnabled") boolean clientEnabled);

    @Query("SELECT t FROM Trade t WHERE t.status IN (:statuses) " +
            "AND t.statusUpdated <= :lastUpdate " +
            "AND t.client.enabled = :clientEnabled " +
            "AND t.nnOrder IS NOT NULL " +
            "ORDER BY t.statusUpdated DESC")
    List<Trade> findNnByStatusInAndStatusUpdatedBefore(
            @Param("statuses") Collection<TradeStatus> statuses,
            @Param("lastUpdate") LocalDateTime lastUpdate,
            @Param("clientEnabled") boolean clientEnabled);

    @Query("SELECT t FROM Trade t WHERE t.client = :client " +
            "AND (t.currencyFrom = :currency OR t.currencyTo = :currency) " +
            "AND (t.status IN (:unknownStatuses) " +
            "OR ((t.status IN (:openedStatuses) AND t.wallet.statusUpdated <= t.statusUpdated)))")
    Collection<Trade> findByWalletKey(
            @Param("client") Client client,
            @Param("currency") TradingCurrency currency,
            @Param("unknownStatuses") Set<TradeStatus> unknownStatuses,
            @Param("openedStatuses") Set<TradeStatus> openedStatuses);

    long countAllByStatusEquals(TradeStatus status);

    long countAllByStatusNotIn(Collection<TradeStatus> status);

    @Query("SELECT COALESCE(SUM(t.openingAmount), 0) FROM Trade t WHERE "
            + "t.client = :client AND t.currencyFrom = :currencyFrom AND t.currencyTo = :currencyTo "
            + "AND t.status IN (:statuses) AND t.ignoreAsSideLimit = FALSE")
    BigDecimal tradeBalanceWithSide(@Param("client") Client client,
                                    @Param("currencyFrom") TradingCurrency currencyFrom,
                                    @Param("currencyTo") TradingCurrency currencyTo,
                                    @Param("statuses") Set<TradeStatus> statuses);

    @Query("SELECT COALESCE(SUM(t.openingAmount * t.openingPrice), 0) FROM Trade t WHERE "
            + "t.wallet = :wallet AND t.status IN (:statuses) AND t.isSell")
    BigDecimal lockedByBuyTradesWithStatus(@Param("wallet") Wallet wallet, @Param("statuses") Set<TradeStatus> statuses);

    @Query("SELECT COALESCE(SUM(-t.openingAmount), 0) FROM Trade t WHERE "
            + "t.wallet = :wallet AND t.status IN (:statuses) AND NOT t.isSell")
    BigDecimal lockedBySellTradesWithStatus(@Param("wallet") Wallet wallet, @Param("statuses") Set<TradeStatus> statuses);

    List<Trade> findByXoOrderNotNull();
    List<Trade> findByNnOrderNotNull();

    Optional<Trade> findByDependsOnId(String id);

    @Query("SELECT t FROM Trade t WHERE "
            + "t.dependsOn.status IN (:orderStatus) AND t.status IN (:dependentStatus)")
    List<Trade> findDependantsByMasterStatus(
            @Param("dependentStatus") Collection<TradeStatus> dependentStatus,
            @Param("orderStatus") Collection<TradeStatus> orderStatus);
}
