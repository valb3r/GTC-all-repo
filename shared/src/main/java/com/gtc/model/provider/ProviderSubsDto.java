package com.gtc.model.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSubsDto {

    private Mode mode;

    public enum Mode {
        TICKER,
        BOOK,
        MARKET_PRICE
    }
}
