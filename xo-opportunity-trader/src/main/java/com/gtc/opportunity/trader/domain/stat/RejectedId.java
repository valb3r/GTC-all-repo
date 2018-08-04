package com.gtc.opportunity.trader.domain.stat;

import com.gtc.opportunity.trader.domain.LongMessageLimiter;
import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * Created by Valentyn Berezin on 04.04.18.
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RejectedId implements Serializable {

    private static final int REASON_LEN = 256;

    @Embedded
    private StatId statId;

    @NotEmpty
    private String reason;

    public void setReason(String reason) {
        this.reason = LongMessageLimiter.trunc(reason, REASON_LEN);
    }
}
