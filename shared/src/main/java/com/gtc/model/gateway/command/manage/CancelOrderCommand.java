package com.gtc.model.gateway.command.manage;

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
public class CancelOrderCommand extends BaseMessage implements WithOrderId {

    public static final String TYPE = "cancel";

    @NotBlank
    private String orderId;

    @Builder
    public CancelOrderCommand(String clientName, String id, String orderId) {
        super(clientName, id);
        this.orderId = orderId;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
