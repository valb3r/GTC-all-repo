package com.gtc.tradinggateway.service.wex.dto;

import com.gtc.model.gateway.data.OrderDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Getter
@Setter
@NoArgsConstructor
public class WexGetOpenResponse extends BaseWexResponse<Map<String, WexGetResponse.Value>> {

    public List<OrderDto> mapTo () {
        return getRet().entrySet().stream()
                .map(it -> WexGetResponse.mapTo(it.getKey(), it.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
