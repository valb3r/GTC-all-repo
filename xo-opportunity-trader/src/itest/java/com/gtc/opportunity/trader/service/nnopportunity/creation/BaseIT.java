package com.gtc.opportunity.trader.service.nnopportunity.creation;

import com.gtc.opportunity.trader.app.AppInitializer;
import com.gtc.opportunity.trader.config.Const;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AppInitializer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Const.SpringProfiles.TEST)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestExecutionListeners(value = BaseIT.ExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public abstract class BaseIT extends BaseMockitoIT {

    // This value ensures that Flyway migration will run before @Sql listener and @Transactional listener
    static final int ORDER = 3000;

    public static class ExecutionListener extends AbstractTestExecutionListener {

        @Override
        public int getOrder() {
            return ORDER;
        }

        @Override
        public void beforeTestMethod(TestContext testContext) {
            log.info("Migrating flyway");
            ApplicationContext appContext = testContext.getApplicationContext();
            Flyway flyWay = appContext.getBean(Flyway.class);
            flyWay.migrate();
        }

        @Override
        public void afterTestMethod(TestContext testContext) {
            log.info("Dropping all");
            ApplicationContext appContext = testContext.getApplicationContext();
            JdbcOperations jdbcOper = appContext.getBean(JdbcOperations.class);
            jdbcOper.update("DROP ALL OBJECTS DELETE FILES");
        }
    }
}
