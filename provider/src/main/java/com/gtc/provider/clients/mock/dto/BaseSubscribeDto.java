package com.gtc.provider.clients.mock.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Data
public class BaseSubscribeDto {

    private final SubsType type;
    private final String symbol;
    private final String exchangeId;
}
