package com.gtc.model.gateway.command.manage;

import com.gtc.model.gateway.BaseMessage;
import lombok.*;
import javax.validation.constraints.NotBlank;

/**
 * Created by Valentyn Berezin on 21.02.18.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ListOpenCommand extends BaseMessage {

    public static final String TYPE = "list";

    @NotBlank
    private String currencyFrom;

    @NotBlank
    private String currencyTo;

    @Builder
    public ListOpenCommand(String clientName, String id, String currencyFrom, String currencyTo) {
        super(clientName, id);
        this.currencyFrom = currencyFrom;
        this.currencyTo = currencyTo;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
