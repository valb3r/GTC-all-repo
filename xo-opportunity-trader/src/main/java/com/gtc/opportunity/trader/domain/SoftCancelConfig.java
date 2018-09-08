package com.gtc.opportunity.trader.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SoftCancelConfig implements Serializable {

    @Id
    private int id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id")
    private ClientConfig clientCfg;

    @OneToOne(mappedBy = "cancelConfig")
    private SoftCancel cancel;

    @Column(name = "wait_m")
    private int waitM;

    private BigDecimal minPriceLossPct;

    private BigDecimal maxPriceLossPct;

    private BigDecimal doneToCancelRatio;

    private boolean enabled;
}
