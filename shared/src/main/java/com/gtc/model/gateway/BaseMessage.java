package com.gtc.model.gateway;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import javax.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Valentyn Berezin on 21.02.18.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class BaseMessage implements Serializable {

    protected static final String MIN_DECIMAL = "0.0000000000000000000001";

    public String type() {
        return null;
    }

    private final long createdTimestamp = System.currentTimeMillis();

    @NotBlank
    private String clientName;

    @NotBlank
    private String id;

    private String type;

    private RetryStrategy retryStrategy;

    public BaseMessage(String clientName, String id) {
        this.clientName = clientName;

        if (null == id) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
    }
}
