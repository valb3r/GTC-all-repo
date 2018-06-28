package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Trade implements Serializable {

    @Id
    private String id;

    @NotBlank
    private String assignedId;

    @NotNull
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Client client;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyFrom;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyTo;

    @ManyToOne
    private AcceptedXoTrade xoOrder;

    @NotNull
    private BigDecimal openingAmount;

    @NotNull
    private BigDecimal openingPrice;

    @NotNull
    @Audited
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull
    @Audited
    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal closingAmount;
    private BigDecimal closingPrice;

    private boolean isSell;

    @NotNull
    @Audited
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    @Audited
    private String responseStatus;

    @Audited
    private String nativeStatus;

    @NotNull
    private LocalDateTime statusUpdated;

    @Version
    private int version;

    @NotNull
    @OneToOne
    private Wallet wallet;

    @Audited
    private String lastMessageId;

    @Audited
    private String lastError;

    private LocalDateTime recordedOn;

    // (i.e. BTC/USD) if sell - how many USD we expect to get/ buy - how many BTC we expect to get
    @NotNull
    private BigDecimal expectedReverseAmount;

    private boolean ignoreAsSideLimit;

    public void setLastError(String lastError) {
        this.lastError = LongMessageLimiter.trunc(lastError);
    }

    // used by lombok gutts
    public static class TradeBuilder {

        private String lastError;

        public TradeBuilder lastError(String lastError) {
            this.lastError = LongMessageLimiter.trunc(lastError);
            return this;
        }
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class EsbKey {

        private final String assignedId;
        private final String clientName;
    }

    public Optional<BigDecimal> amountReservedOnWallet(Wallet wallet) {
        boolean walletFrom = wallet.getCurrency() == currencyFrom;
        boolean walletTo = wallet.getCurrency() == currencyTo;

        if (!walletFrom && !walletTo) {
            return Optional.empty();
        }

        if (isSell && walletFrom) {
            return Optional.of(amount);
        } else if (!isSell && walletTo) {
            return Optional.of(expectedReverseAmount);
        }

        return Optional.of(BigDecimal.ZERO);
    }
}
