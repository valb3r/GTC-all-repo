package com.gtc.opportunity.trader.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Valentyn Berezin on 04.09.18.
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SoftCancel implements Serializable {

    @Id
    private int id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id")
    private SoftCancelConfig cancelConfig;

    private int done;
    private int cancelled;
}
