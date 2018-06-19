package com.gtc.model.gateway.data;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@Data
@Builder
public class OrderDto implements Serializable {

    private String orderId;
    private BigDecimal size;
    private BigDecimal price;
    private OrderStatus status;
    private String statusString;
}
