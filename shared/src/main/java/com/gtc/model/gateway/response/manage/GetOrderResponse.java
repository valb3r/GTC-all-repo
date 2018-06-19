package com.gtc.model.gateway.response.manage;

import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.WithOrderId;
import com.gtc.model.gateway.data.OrderDto;
import lombok.*;

/**
 * Created by Valentyn Berezin on 21.02.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class GetOrderResponse extends BaseMessage implements WithOrderId {

    public static final String TYPE = "resp.get";

    @lombok.experimental.Delegate
    private OrderDto order;

    @Builder
    public GetOrderResponse(String clientName, String id, OrderDto order) {
        super(clientName, id);
        this.order = order;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
