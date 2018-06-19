package com.gtc.model.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Valentyn Berezin on 15.06.18.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeDto {

    private String client;
    private Mode mode;

    public enum Mode {
        TICKER,
        BOOK
    }
}
