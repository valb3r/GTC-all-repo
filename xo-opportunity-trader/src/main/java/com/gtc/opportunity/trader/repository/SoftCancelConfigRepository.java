package com.gtc.opportunity.trader.repository;

import com.gtc.opportunity.trader.domain.SoftCancelConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Repository
public interface SoftCancelConfigRepository extends CrudRepository<SoftCancelConfig, Integer> {

    @Query("SELECT sc FROM SoftCancelConfig sc JOIN sc.clientCfg cc " +
            "WHERE cc.client.enabled = TRUE AND cc.enabled = TRUE AND sc.enabled = TRUE")
    List<SoftCancelConfig> findAllActive();
}
