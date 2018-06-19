package com.gtc.tradinggateway.service.huobi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by Valentyn Berezin on 14.04.18.
 */
@Data
public class HuobiGetListResponseDto {

    @JsonProperty("data")
    private List<HuobiOrderDto> orders;
}
