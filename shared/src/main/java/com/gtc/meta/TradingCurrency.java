package com.gtc.meta;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
public enum TradingCurrency {
    Usd("USD"),
    Eur("EUR"),
    Bitcoin("BTC"),
    Ripple("XRP"),
    Ethereum("ETH"),
    BitcoinCash("BCH"),
    Litecoin("LTC"),
    Cardano("ADA"),
    IOTA("MIOTA"),
    NEM("XEM"),
    Dash("DASH"),
    Monero("XMR"),
    EOS("EOS"),
    Stellar("XLM"),
    BitcoinGold("BTG"),
    NEO("NEO"),
    Qtum("QTUM"),
    EthereumClassic("ETC"),
    BitConnect("BCC"),
    Lisk("LSK"),
    TRON("TRX"),
    Verge("XVG"),
    RaiBlocks("XRB"),
    ICON("ICX"),
    Ardor("ARDR"),
    Zcash("ZEC"),
    OmiseGO("OMG"),
    BitShares("BTS"),
    Stratis("STRAT"),
    Populous("PPT"),
    Tether("USDT"),
    Waves("WAVES"),
    Hshare("HSR"),
    Bytecoin("BCN"),
    Komodo("KMD"),
    Siacoin("SC"),
    Dogecoin("DOGE"),
    BinanceCoin("BNB"),
    Augur("REP"),
    SALT("SALT"),
    Veritaseum("VERI"),
    Steem("STEEM"),
    Ark("ARK"),
    Golem("GNT"),
    DigiByte("DGB"),
    VeChain("VEN"),
    PIVX("PIVX"),
    Nxt("NXT"),
    MonaCoin("MONA"),
    Decred("DCR"),
    Status("SNT"),
    ByteballBytes("GBYTE"),
    Nano("NANO"),
    Gas("GAS"),
    InvalidCurrency("~Invalid");

    @Getter
    private final String code;

    TradingCurrency(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }

    public static TradingCurrency fromCode(String code) {
        return Stream.of(TradingCurrency.values())
                .filter(thresholdType -> thresholdType.getCode().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mapping for " + code + " not found"));
    }
}
