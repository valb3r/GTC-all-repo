package com.gtc.opportunity.trader.repository;

import com.gtc.opportunity.trader.domain.SoftCancel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Repository
public interface SoftCancelRepository extends CrudRepository<SoftCancel, Integer> {

    @Query("SELECT sc FROM SoftCancel sc JOIN sc.clientCfg cc " +
            "WHERE cc.client.enabled = TRUE AND cc.enabled = TRUE AND sc.enabled = TRUE")
    List<SoftCancel> findAllActive();
}
