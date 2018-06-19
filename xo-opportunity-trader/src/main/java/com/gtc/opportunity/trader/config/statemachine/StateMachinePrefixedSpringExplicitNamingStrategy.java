package com.gtc.opportunity.trader.config.statemachine;

import com.google.common.collect.ImmutableMap;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;

import java.util.Map;

import static com.gtc.opportunity.trader.config.statemachine.Const.PREFIX;

/**
 * Hacking absence of adding prefix to table names in StateMachine.
 */
public class StateMachinePrefixedSpringExplicitNamingStrategy extends SpringPhysicalNamingStrategy {

    private static final Map<String, String> REMAP = ImmutableMap.<String, String>builder()
            .put("action", PREFIX + "Action")
            .put("statestateactions", PREFIX + "StateStateActions")
            .put("deferredevents", PREFIX + "DeferredEvents")
            .put("guard", PREFIX + "Guard")
            .put("state", PREFIX + "State")
            .put("statemachine", PREFIX + "StateMachine")
            .put("stateentryactions", PREFIX + "StateEntryActions")
            .put("stateexitactions", PREFIX + "StateExitActions")
            .put("transition", PREFIX + "Transition")
            .put("transitionactions", PREFIX + "TransitionActions")
            .build();

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        String mapped = REMAP.get(name.getText().replace("_", "").toLowerCase());
        String id = null != mapped ? mapped : name.getText();

        return super.toPhysicalTableName(
                Identifier.toIdentifier(id, name.isQuoted()),
                jdbcEnvironment
        );
    }
}
