package com.gtc.opportunity.trader.domain.stat.rejected;

import com.gtc.opportunity.trader.domain.stat.RejectedId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Entity
@Getter
@Setter
@Table(name = "xo_trade_rejected_stat")
@Inheritance
@NoArgsConstructor
@DiscriminatorColumn(name = "kind")
public abstract class BaseXoTradeRejectedStat {

    @EmbeddedId
    @lombok.experimental.Delegate
    private RejectedId id;

    @Version
    private int version;

    @Embedded
    @lombok.experimental.Delegate
    private RejectionStat rejections;

    @Column(updatable = false, insertable = false)
    private String kind;

    @NotNull
    private LocalDateTime updatedAt;
}
