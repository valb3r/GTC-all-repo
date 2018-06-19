package com.gtc.model.gateway.response.manage;

import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.data.OrderDto;
import lombok.*;

import java.util.List;

/**
 * Created by Valentyn Berezin on 21.02.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ListOpenOrdersResponse extends BaseMessage {

    public static final String TYPE = "resp.listOpen";

    private List<OrderDto> orders;

    @Builder
    public ListOpenOrdersResponse(String clientName, String id, List<OrderDto> orders) {
        super(clientName, id);
        this.orders = orders;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
