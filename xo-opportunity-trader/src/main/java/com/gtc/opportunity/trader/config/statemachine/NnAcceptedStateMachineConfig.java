package com.gtc.opportunity.trader.config.statemachine;

import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.service.statemachine.nnaccept.NnAcceptMachine;
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

import static com.gtc.opportunity.trader.config.statemachine.NnAcceptedStateMachineConfig.NN_ACCEPTED_MACHINE_FACTORY;
import static com.gtc.opportunity.trader.domain.AcceptEvent.*;
import static com.gtc.opportunity.trader.domain.NnAcceptStatus.*;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@Slf4j
@Configuration
@EnableStateMachineFactory(name = NN_ACCEPTED_MACHINE_FACTORY)
public class NnAcceptedStateMachineConfig extends StateMachineConfigurerAdapter<NnAcceptStatus, AcceptEvent> {

    public static final String NN_MACHINE_SERVICE = "NN_MACHINE_SERVICE";
    public static final String NN_ACCEPTED_MACHINE_FACTORY = "NN_ACCEPTED_MACHINE_FACTORY";
    private static final String NN_PERSISTOR = "NN_ACCEPTED_PERSISTOR";

    private final StateMachineRuntimePersister<NnAcceptStatus, AcceptEvent, String> persister;
    private final NnAcceptMachine machine;

    public NnAcceptedStateMachineConfig(
            @Qualifier(NN_PERSISTOR) StateMachineRuntimePersister<NnAcceptStatus, AcceptEvent, String> persister,
            @Lazy NnAcceptMachine machine) {
        this.persister = persister;
        this.machine = machine;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<NnAcceptStatus, AcceptEvent> config) throws Exception {
        config
                .withPersistence()
                .runtimePersister(persister);
    }

    @Override
    public void configure(StateMachineStateConfigurer<NnAcceptStatus, AcceptEvent>states) throws Exception {
        states.withStates()
                .initial(MASTER_UNKNOWN)
                .state(MASTER_OPENED)
                .state(MASTER_ISSUE)
                .state(PENDING_SLAVE)
                .state(SLAVE_UNKNOWN)
                .state(SLAVE_OPENED)
                .state(ERROR)
                .end(DONE);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<NnAcceptStatus, AcceptEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .event(TRADE_ACK).source(MASTER_UNKNOWN).target(MASTER_OPENED).action(machine::ack, machine::error)
                .and().withExternal()
                .event(TRADE_DONE).source(MASTER_UNKNOWN).target(PENDING_SLAVE).action(machine::ack, machine::error)
                .and().withExternal()
                .event(TRADE_DONE).source(MASTER_OPENED).target(PENDING_SLAVE).action(machine::ack, machine::error)
                .and().withExternal()
                .event(ISSUE).source(MASTER_UNKNOWN).target(MASTER_ISSUE).action(machine::tradeError, machine::error)
                .and().withExternal()
                .event(CONTINUE).source(PENDING_SLAVE).target(SLAVE_UNKNOWN).action(machine::pushedSlave, machine::error)
                .and().withExternal()
                .event(TRADE_ACK).source(SLAVE_UNKNOWN).target(SLAVE_OPENED).action(machine::ack, machine::error)
                .and().withExternal()
                .event(TRADE_DONE).source(SLAVE_UNKNOWN).target(DONE).action(machine::done, machine::error)
                .and().withExternal()
                .event(TRADE_DONE).source(SLAVE_OPENED).target(DONE).action(machine::done, machine::error)
                .and().withExternal()
                .event(ISSUE).source(SLAVE_UNKNOWN).target(SLAVE_ISSUE).action(machine::tradeError, machine::error)
                .and().withExternal()
                .event(CANCEL).source(MASTER_UNKNOWN).target(ABORTED).action(machine::abort, machine::error)
                .and().withExternal()
                .event(CANCEL).source(MASTER_OPENED).target(ABORTED).action(machine::abort, machine::error)
                .and().withExternal()
                .event(CANCEL).source(SLAVE_UNKNOWN).target(ABORTED).action(machine::abort, machine::error)
                .and().withExternal()
                .event(CANCEL).source(SLAVE_OPENED).target(ABORTED).action(machine::abort, machine::error);
    }

    @Bean(NN_MACHINE_SERVICE)
    public StateMachineService<NnAcceptStatus, AcceptEvent> stateMachineService(
            @Qualifier(NN_ACCEPTED_MACHINE_FACTORY) StateMachineFactory<NnAcceptStatus, AcceptEvent> stateMachineFactory,
            StateMachinePersist<NnAcceptStatus, AcceptEvent, String> persist) {
        return new DefaultStateMachineService<>(stateMachineFactory, persist);
    }

    @Configuration
    public static class NnMachineRuntimePersister {

        @Bean(name = NN_PERSISTOR)
        public StateMachineRuntimePersister<NnAcceptStatus, AcceptEvent, String> stateMachineRuntimePersister(
                JpaStateMachineRepository jpaStateMachineRepository) {
            return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
        }
    }
}
