package com.gtc.opportunity.trader.config.statemachine;

import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.service.statemachine.trade.TradeMachine;
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

import static com.gtc.opportunity.trader.config.statemachine.TradeStateMachineConfig.TRADE_MACHINE_FACTORY;
import static com.gtc.opportunity.trader.domain.TradeStatus.*;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@Slf4j
@Configuration
@EnableStateMachineFactory(name = TRADE_MACHINE_FACTORY)
public class TradeStateMachineConfig extends StateMachineConfigurerAdapter<TradeStatus, TradeEvent> {

    public static final String TRADE_MACHINE_SERVICE = "TRADE_MACHINE_SERVICE";
    public static final String TRADE_MACHINE_FACTORY = "TRADE_MACHINE_FACTORY";
    private static final String TRADE_MACHINE_PERSISTOR = "TRADE_MACHINE_PERSISTOR";

    private final StateMachineRuntimePersister<TradeStatus, TradeEvent, String> persister;
    private TradeMachine machine;

    public TradeStateMachineConfig(
            @Qualifier(TRADE_MACHINE_PERSISTOR) StateMachineRuntimePersister<TradeStatus, TradeEvent, String> persister,
            @Lazy TradeMachine tradeMachine) { // breaking circular dep. StateMachineFactoryDelegatingFactoryBean
        this.persister = persister;
        this.machine = tradeMachine;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<TradeStatus, TradeEvent> config) throws Exception {
        config
                .withPersistence()
                .runtimePersister(persister);
    }

    @Override
    public void configure(StateMachineStateConfigurer<TradeStatus, TradeEvent> states) throws Exception {
        states.withStates()
                .initial(DEPENDS_ON)
                .state(UNKNOWN)
                .state(ERR_OPEN)
                .state(NEED_RETRY)
                .state(OPENED)
                .state(CANCELLED)
                .state(GEN_ERR)
                .end(CLOSED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TradeStatus, TradeEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .event(TradeEvent.DEPENDENCY_DONE).source(DEPENDS_ON).target(UNKNOWN).action(machine::doneDependency)
                .and().withExternal()
                .event(TradeEvent.CANCELLED).source(DEPENDS_ON).target(CANCELLED).action(machine::cancel)
                .and().withExternal()
                .event(TradeEvent.ACK).source(UNKNOWN).target(OPENED).action(machine::ack)
                .and().withExternal()
                .event(TradeEvent.DONE).source(OPENED).target(CLOSED).action(machine::done)
                .and().withExternal()
                .event(TradeEvent.DONE).source(UNKNOWN).target(CLOSED).action(machine::done)
                .and().withExternal()
                .event(TradeEvent.CANCELLED).source(OPENED).target(CANCELLED).action(machine::cancel)
                .and().withExternal()
                .event(TradeEvent.CANCELLED).source(UNKNOWN).target(CANCELLED).action(machine::cancel)
                .and().withExternal()
                .event(TradeEvent.TRANSIENT_ERR).source(UNKNOWN).target(NEED_RETRY).action(machine::transientError)
                .and().withExternal()
                .event(TradeEvent.TIMEOUT).source(UNKNOWN).target(NEED_RETRY).action(machine::timeout)
                .and().withExternal()
                .event(TradeEvent.RETRY).source(NEED_RETRY).target(UNKNOWN).action(machine::retry)
                .and().withExternal()
                .event(TradeEvent.ACK).source(NEED_RETRY).target(OPENED).action(machine::ack)
                .and().withExternal()
                .event(TradeEvent.ERROR).source(UNKNOWN).target(ERR_OPEN).action(machine::fatalError)
                .and().withExternal()
                .event(TradeEvent.ERROR).source(OPENED).target(GEN_ERR).action(machine::fatalError);
    }

    @Bean(name = TRADE_MACHINE_SERVICE)
    public StateMachineService<TradeStatus, TradeEvent> stateMachineService(
            @Qualifier(TRADE_MACHINE_FACTORY) StateMachineFactory<TradeStatus, TradeEvent> stateMachineFactory,
            StateMachinePersist<TradeStatus, TradeEvent, String> persist) {
        return new DefaultStateMachineService<>(stateMachineFactory, persist);
    }

    @Configuration
    public static class RuntimePersister {

        @Bean(name = TRADE_MACHINE_PERSISTOR)
        public StateMachineRuntimePersister<TradeStatus, TradeEvent, String> stateMachineRuntimePersister(
                JpaStateMachineRepository jpaStateMachineRepository) {
            return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
        }
    }
}
