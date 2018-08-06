package com.gtc.model.gateway.response;

import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.WithOrderId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import javax.validation.constraints.NotBlank;

/**
 * Created by Valentyn Berezin on 27.02.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ErrorResponse extends BaseMessage implements WithOrderId {

    public static final String TYPE = "resp.error";

    private String orderId;

    @NotBlank
    private String onMessageId;

    @NotBlank
    private String errorCause;

    private boolean isTransient;

    @NotBlank
    private String occurredOn;

    @NotBlank
    private String occurredOnType;

    @Override
    public String type() {
        return TYPE;
    }
}
