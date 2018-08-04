package com.gtc.model.gateway.command.create;

import com.gtc.model.gateway.BaseMessage;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Transaction (in terms of receiving)-like creation of multiple commands.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class MultiOrderCreateCommand extends BaseMessage {

    public static final String TYPE = "createMulti";

    @NotEmpty
    private Set<CreateOrderCommand> commands;

    @Builder
    public MultiOrderCreateCommand(String clientName, String id, Set<CreateOrderCommand> commands) {
        super(clientName, id);
        this.commands = commands;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
