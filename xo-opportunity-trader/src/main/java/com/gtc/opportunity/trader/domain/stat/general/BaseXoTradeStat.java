package com.gtc.opportunity.trader.domain.stat.general;

import com.gtc.opportunity.trader.domain.stat.StatId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 26.02.18.
 */
@Entity
@Getter
@Setter
@Table(name = "xo_trade_stat")
@Inheritance
@DiscriminatorColumn(name = "kind", length = 64)
@ToString
public abstract class BaseXoTradeStat implements Serializable {

    @EmbeddedId
    @Delegate
    private StatId id;

    @Embedded
    @Delegate
    private Snapshot snapshot;

    @Version
    private int version;

    @Column(insertable = false, updatable = false)
    private String kind;

    @NotNull
    private LocalDateTime updatedAt;
}
