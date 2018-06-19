package com.gtc.model.gateway.response.manage;

import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.WithOrderId;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Created by Valentyn Berezin on 21.02.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class CancelOrderResponse extends BaseMessage implements WithOrderId {

    public static final String TYPE = "resp.cancel";

    @NotBlank
    private String orderId;

    @Builder
    public CancelOrderResponse(String clientName, String id, String orderId) {
        super(clientName, id);
        this.orderId = orderId;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
