package com.gtc.model.gateway.response.account;

import com.gtc.model.gateway.BaseMessage;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 03.03.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class GetAllBalancesResponse extends BaseMessage {

    public static final String TYPE = "resp.getAllBalances";

    private Map<String, BigDecimal> balances;

    @Override
    public String type() {
        return TYPE;
    }

    @Builder
    public GetAllBalancesResponse(String clientName, String id, Map<String, BigDecimal> balances) {
        super(clientName, id);
        this.balances = balances;
    }
}
