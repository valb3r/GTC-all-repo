package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.dto.ByClientAndPair;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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
    Set<ByClientAndPair> findSymbolsWithActiveOrders(
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

    @Query("SELECT t FROM Trade t WHERE t.client = :client " +
            "AND (t.currencyFrom = :currency OR t.currencyTo = :currency) " +
            "AND (t.status IN (:unknownStatuses) " +
            "OR ((t.status IN (:openedStatuses) AND t.wallet.statusUpdated <= t.statusUpdated)))")
    Collection<Trade> findByWalletKey(
            @Param("client") Client client,
            @Param("currency") TradingCurrency currency,
            @Param("unknownStatuses") Set<TradeStatus> unknownStatuses,
            @Param("openedStatuses") Set<TradeStatus> openedStatuses);

    @Query("SELECT t FROM Trade t WHERE "
            + "t.status = :status " +
            "AND t.statusUpdated = (SELECT MAX(t.statusUpdated) FROM Trade t WHERE t.status = :status)")
    Optional<Trade> findLatestByStatus(@Param("status") TradeStatus status);

    long countAllByStatusEquals(TradeStatus status);

    long countAllByStatusNotIn(Collection<TradeStatus> status);
}
