FROM openjdk:8
VOLUME /tmp
RUN mkdir -p /var/app
WORKDIR /var/app

ADD build/libs/provider.jar /var/app/provider.jar
RUN bash -c 'touch /var/app/provider.jar'

RUN cd /var/app \
    && wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
     && unzip newrelic-java.zip && rm newrelic-java.zip

CMD java \
    -javaagent:/var/app/newrelic/newrelic.jar \
    -Dnewrelic.config.agent_enabled="${ENABLE_NEWRELIC:-false}" \
    -Dnewrelic.config.environment=Prod -Dnewrelic.config.app_name=BidCacher \
    -Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF8 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=15005 \
    -jar provider.jar

EXPOSE 8080
EXPOSE 9092
EXPOSE 15005
