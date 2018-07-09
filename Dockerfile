FROM openjdk:8
VOLUME /tmp
RUN mkdir -p /var/app
WORKDIR /var/app

ADD all.sh all.sh
ADD provider/build/libs/provider-1.0-SNAPSHOT.jar provider.jar
ADD gateway/build/libs/gateway-1.0-SNAPSHOT.jar gateway.jar
ADD xo-opportunity-trader/build/libs/xo-opportunity-trader-1.0-SNAPSHOT.jar opportunity-trader.jar
ADD persistor/build/libs/persistor-1.0-SNAPSHOT.jar persistor.jar

RUN bash -c 'touch /var/app/provider.jar'
RUN bash -c 'touch /var/app/gateway.jar'
RUN bash -c 'touch /var/app/opportunity-trader.jar'
RUN bash -c 'touch /var/app/persistor.jar'

RUN cd /var/app \
    && wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
     && unzip newrelic-java.zip && rm newrelic-java.zip

CMD ./all.sh

EXPOSE 8080
EXPOSE 8084
