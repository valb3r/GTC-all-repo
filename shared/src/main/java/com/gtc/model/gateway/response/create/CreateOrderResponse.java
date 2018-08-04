package com.gtc.model.gateway.response.create;

import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.WithOrderId;
import lombok.*;
import javax.validation.constraints.NotBlank;

/**
 * Created by Valentyn Berezin on 21.02.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class CreateOrderResponse extends BaseMessage implements WithOrderId {

    public static final String TYPE = "resp.create";

    @NotBlank
    private String requestOrderId;

    // not all exchanges allow to have client-assigned id
    @NotBlank
    private String orderId;

    // order can be executed immediately on creation
    private boolean isExecuted;

    @Builder
    public CreateOrderResponse(String clientName, String id, String requestOrderId, String orderId,
                               boolean isExecuted) {
        super(clientName, id);
        this.requestOrderId = requestOrderId;
        this.orderId = orderId;
        this.isExecuted = isExecuted;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
