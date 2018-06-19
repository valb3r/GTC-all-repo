package com.gtc.tradinggateway.service.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Data
@Builder
public class OrderCreatedDto {

    private String requestedId;
    private String assignedId;
    private boolean isExecuted;
}
