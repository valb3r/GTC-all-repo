#!/bin/bash

scp gateway/build/libs/gateway-1.0-SNAPSHOT.jar ${SCP_USER}@${SCP_IP}:/home/${SCP_USER}/gateway.jar
scp provider/build/libs/provider-1.0-SNAPSHOT.jar ${SCP_USER}@${SCP_IP}:/home/${SCP_USER}/provider.jar
scp persistor/build/libs/persistor-1.0-SNAPSHOT.jar ${SCP_USER}@${SCP_IP}:/home/${SCP_USER}/persistor.jar
scp xo-opportunity-trader/build/libs/xo-opportunity-trader-1.0-SNAPSHOT.jar ${SCP_USER}@${SCP_IP}:/home/${SCP_USER}/opportunity-trader.jar

rm -rf newrelic && mkdir newrelic && cd newrelic \
&& wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
&& scp -r newrelic-java.zip ${SCP_USER}@${SCP_IP}:/home/${SCP_USER}/newrelic.zip \
&& cd .. && rm -rf newrelic

scp all.sh ${SCP_USER}@${SCP_IP}:/home/${SCP_USER}/all.sh
