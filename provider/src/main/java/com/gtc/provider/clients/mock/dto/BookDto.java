package com.gtc.provider.clients.mock.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Data
public class BookDto {

    private final String symbol;
    private final BigDecimal price;
    private final BigDecimal amount;
}
