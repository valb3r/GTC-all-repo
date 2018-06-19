package com.gtc.opportunity.trader.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
@Service
@RequiredArgsConstructor
public class CurrentTimestamp {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public LocalDateTime dbNow() {
        return ((Timestamp) entityManager.createNativeQuery(
                "SELECT CURRENT_TIMESTAMP FROM dual")
                .getSingleResult())
                .toLocalDateTime();
    }
}
