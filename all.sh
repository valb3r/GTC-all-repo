#!/usr/bin/env bash

if [ -f newrelic.zip ]; then
    rm -rf newrelic
    unzip newrelic.zip
fi

java \
    -javaagent:newrelic/newrelic.jar \
    -Dnewrelic.config.agent_enabled="${ENABLE_NEWRELIC:-false}" \
    -Dnewrelic.config.environment=Prod -Dnewrelic.config.app_name=BookProvider \
    -Xms64m -Xmx128m -Xss256k \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF8 \
    -jar provider.jar &

java \
    -javaagent:newrelic/newrelic.jar \
    -Dnewrelic.config.agent_enabled="${ENABLE_NEWRELIC:-false}" \
    -Dnewrelic.config.environment=Prod -Dnewrelic.config.app_name=TradeGateway \
    -Xms32m -Xmx64m -XX:-TieredCompilation -Xss256k -XX:+UseStringDeduplication -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF8 \
    -jar gateway.jar &

java \
    -javaagent:newrelic/newrelic.jar \
    -Dnewrelic.config.agent_enabled="${ENABLE_NEWRELIC:-false}" \
    -Dnewrelic.config.environment=Prod -Dnewrelic.config.app_name=XoOpportunityTrader \
    -Xms128m -Xmx384m -Xss256k \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF8 \
    -jar opportunity-trader.jar &

java \
    -javaagent:newrelic/newrelic.jar \
    -Dnewrelic.config.agent_enabled="${ENABLE_NEWRELIC:-false}" \
    -Dnewrelic.config.environment=Prod -Dnewrelic.config.app_name=Persistor \
    -Xms32m -Xmx128m -Xss256k \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF8 \
    -jar persistor.jar &

wait &
