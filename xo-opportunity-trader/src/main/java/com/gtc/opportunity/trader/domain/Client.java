package com.gtc.opportunity.trader.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;

import static com.gtc.opportunity.trader.domain.Const.CLIENT_NAME;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Entity
@Getter
@Setter
@ToString(exclude = {"config", "wallet"})
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class Client implements Serializable {

    @Id
    private String name;

    private boolean enabled;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = CLIENT_NAME)
    private Collection<ClientConfig> config;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = CLIENT_NAME)
    private Collection<Wallet> wallet;
}
