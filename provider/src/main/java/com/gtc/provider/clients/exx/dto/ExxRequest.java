package com.gtc.provider.clients.exx.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Created by mikro on 14.01.2018.
 */
@Data
@RequiredArgsConstructor
public class ExxRequest {

    private final String dataType;
    private final int dataSize;
    private final String action = "ADD";
}
