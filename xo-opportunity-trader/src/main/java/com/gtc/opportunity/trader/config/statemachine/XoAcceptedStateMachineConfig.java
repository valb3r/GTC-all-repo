package com.gtc.opportunity.trader.config.statemachine;

import com.gtc.opportunity.trader.domain.XoAcceptEvent;
import com.gtc.opportunity.trader.domain.XoAcceptStatus;
import com.gtc.opportunity.trader.service.statemachine.xoaccept.XoAcceptMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

import static com.gtc.opportunity.trader.config.statemachine.XoAcceptedStateMachineConfig.XO_ACCEPTED_MACHINE_FACTORY;
import static com.gtc.opportunity.trader.domain.XoAcceptStatus.*;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@Slf4j
@Configuration
@EnableStateMachineFactory(name = XO_ACCEPTED_MACHINE_FACTORY)
public class XoAcceptedStateMachineConfig extends StateMachineConfigurerAdapter<XoAcceptStatus, XoAcceptEvent> {

    public static final String XO_MACHINE_SERVICE = "XO_MACHINE_SERVICE";
    public static final String XO_ACCEPTED_MACHINE_FACTORY = "XO_ACCEPTED_MACHINE_FACTORY";
    private static final String XO_PERSISTOR = "XO_ACCEPTED_PERSISTOR";

    private final StateMachineRuntimePersister<XoAcceptStatus, XoAcceptEvent, String> persister;
    private final XoAcceptMachine machine;

    public XoAcceptedStateMachineConfig(
            @Qualifier(XO_PERSISTOR) StateMachineRuntimePersister<XoAcceptStatus, XoAcceptEvent, String> persister,
            @Lazy XoAcceptMachine machine) {
        this.persister = persister;
        this.machine = machine;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<XoAcceptStatus, XoAcceptEvent> config) throws Exception {
        config
                .withPersistence()
                .runtimePersister(persister);
    }

    @Override
    public void configure(StateMachineStateConfigurer<XoAcceptStatus, XoAcceptEvent>states) throws Exception {
        states.withStates()
                .initial(UNCONFIRMED)
                .state(ACK_PART)
                .state(ACK_BOTH)
                .state(TRADE_ISSUE)
                .state(DONE_PART)
                .choice(DONE_BOTH)
                .state(REPLENISH)
                .state(REPL_ACK_PART)
                .state(REPL_ACK_BOTH)
                .state(REPL_TRADE_ISSUE)
                .state(REPL_DONE_PART)
                .state(REPL_DONE_BOTH)
                .state(ERROR)
                .end(DONE);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<XoAcceptStatus, XoAcceptEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .event(XoAcceptEvent.TRADE_ACK).source(UNCONFIRMED).target(ACK_PART).action(machine::ack, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ACK).source(ACK_PART).target(ACK_BOTH).action(machine::ack, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(ACK_PART).target(DONE_PART).action(machine::done, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(UNCONFIRMED).target(DONE_PART).action(machine::done, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(ACK_BOTH).target(DONE_PART).action(machine::done, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(DONE_PART).target(DONE_BOTH).action(machine::tradeComplete, machine::error)
                .and().withChoice()
                    .source(DONE_BOTH)
                    .first(REPLENISH, machine::canReplenish, machine::replenish, machine::error)
                .last(DONE, machine::complete, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ACK).source(REPLENISH).target(REPL_ACK_PART).action(machine::ack, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ACK).source(REPL_ACK_PART).target(REPL_ACK_BOTH).action(machine::ack, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(REPLENISH).target(REPL_DONE_PART).action(machine::done, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(REPL_ACK_PART).target(REPL_DONE_PART).action(machine::done, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(REPL_ACK_BOTH).target(REPL_DONE_PART).action(machine::done, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_DONE).source(REPL_DONE_PART).target(DONE).action(machine::replenishmentComplete, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ISSUE).source(UNCONFIRMED).target(TRADE_ISSUE).action(machine::tradeError, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ISSUE).source(ACK_PART).target(TRADE_ISSUE).action(machine::tradeError, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ISSUE).source(REPLENISH).target(REPL_TRADE_ISSUE).action(machine::tradeError, machine::error)
                .and().withExternal()
                .event(XoAcceptEvent.TRADE_ISSUE).source(REPL_ACK_PART).target(REPL_TRADE_ISSUE).action(machine::tradeError, machine::error);
    }

    @Bean(XO_MACHINE_SERVICE)
    public StateMachineService<XoAcceptStatus, XoAcceptEvent> stateMachineService(
            @Qualifier(XO_ACCEPTED_MACHINE_FACTORY) StateMachineFactory<XoAcceptStatus, XoAcceptEvent> stateMachineFactory,
            StateMachinePersist<XoAcceptStatus, XoAcceptEvent, String> persist) {
        return new DefaultStateMachineService<>(stateMachineFactory, persist);
    }

    @Configuration
    public static class XoMachineRuntimePersister {

        @Bean(name = XO_PERSISTOR)
        public StateMachineRuntimePersister<XoAcceptStatus, XoAcceptEvent, String> stateMachineRuntimePersister(
                JpaStateMachineRepository jpaStateMachineRepository) {
            return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
        }
    }
}
