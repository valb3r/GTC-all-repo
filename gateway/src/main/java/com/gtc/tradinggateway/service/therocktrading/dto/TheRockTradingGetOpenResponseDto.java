package com.gtc.tradinggateway.service.therocktrading.dto;

import lombok.Data;

import java.util.List;

@Data
public class TheRockTradingGetOpenResponseDto {

    private List<TheRockTradingOrderDto> orders;
}
