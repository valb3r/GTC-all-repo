package com.gtc.model.provider;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by Valentyn Berezin on 15.06.18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubscribeStreamDto extends ProviderSubsDto {

    private String client;

    public SubscribeStreamDto(Mode mode, String client) {
        super(mode);
        this.client = client;
    }
}
