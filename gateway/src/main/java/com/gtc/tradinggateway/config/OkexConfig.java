package com.gtc.tradinggateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableList;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import com.gtc.tradinggateway.service.okex.OkexEncryptionService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.OKEX;

/**
 * Created by Valentyn Berezin on 23.06.18.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + OKEX)
public class OkexConfig extends BaseConfig {

    public OkexConfig(ConfigFactory factory) {
        mapper = factory.defaultMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        restTemplate = factory.defaultRestTemplate(mapper);
        restTemplate.setMessageConverters(ImmutableList.of(
                new CustomMapping(
                        mapper,
                        params -> OkexEncryptionService.generateSignature(publicKey, secretKey, params))
        ));
    }

    // okex returns form-encoded but in reality it is JSON
    private static class CustomMapping extends FormHttpMessageToPojoConverter {

        CustomMapping(ObjectMapper mapper, Function<Parameters, Map<String, String>> signer) {
            super(mapper, signer);
        }

        @Override
        protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
            return mapper.readValue(inputMessage.getBody(), clazz);
        }
    }
}
