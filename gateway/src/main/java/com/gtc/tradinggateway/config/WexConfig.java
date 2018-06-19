package com.gtc.tradinggateway.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableList;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.WEX;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + WEX)
public class WexConfig extends BaseConfig {

    public WexConfig(ConfigFactory factory) {
        mapper = factory.defaultMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        List<MediaType> supported = new ArrayList<>(converter.getSupportedMediaTypes());
        supported.add(MediaType.TEXT_HTML);
        converter.setSupportedMediaTypes(supported);

        restTemplate = factory.defaultRestTemplate(mapper);
        restTemplate.setMessageConverters(ImmutableList.of(new FormHttpMessageToPojoConverter(mapper), converter));
    }
}
