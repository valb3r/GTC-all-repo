package com.gtc.model.gateway.command.account;

import com.gtc.model.gateway.BaseMessage;
import lombok.*;

/**
 * Created by Valentyn Berezin on 03.03.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class GetAllBalancesCommand extends BaseMessage {

    public static final String TYPE = "getAllBalances";

    @Builder
    public GetAllBalancesCommand(String clientName, String id) {
        super(clientName, id);
    }

    @Override
    public String type() {
        return TYPE;
    }
}
