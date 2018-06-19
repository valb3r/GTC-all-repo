package com.gtc.provider.clients;

import com.gtc.meta.CurrencyPair;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "id")
public class ChannelDto {

    private final String id;
    private CurrencyPair pair;

    public ChannelDto(long id) {
        this.id = String.valueOf(id);
    }

    public ChannelDto(long id, CurrencyPair pair) {
        this.id = String.valueOf(id);
        this.pair = pair;
    }

    public ChannelDto(String id, CurrencyPair pair) {
        this.id = id;
        this.pair = pair;
    }
}
