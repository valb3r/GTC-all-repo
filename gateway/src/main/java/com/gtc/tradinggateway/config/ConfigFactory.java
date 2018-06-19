package com.gtc.tradinggateway.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by mikro on 08.02.2018.
 */
@Service
public class ConfigFactory {

    @Value("${RESPONSE_BODY_TRACE_ENABLED:false}")
    private boolean traceResponseBodyEnabled;

    @Value("${REQUEST_TRACE_ENABLED:false}")
    private boolean traceEnabled;

    ObjectMapper defaultMapper() {
        return new ObjectMapper(new JsonFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
                .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
    }

    RestTemplate defaultRestTemplate(ObjectMapper mapper) {
        RestTemplate template = new RestTemplate(ImmutableList.of(new MappingJackson2HttpMessageConverter(mapper)));

        if (traceEnabled) {
            template.setRequestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
            template.getInterceptors().add(new FullLoggingInterceptor());
        }

        if (!traceEnabled && traceResponseBodyEnabled) {
            template.setRequestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
            template.getInterceptors().add(new ResponseBodyLoggingInterceptor());
        }

        return template;
    }

    @Slf4j
    public static class FullLoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        @SneakyThrows
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)  {
            ClientHttpResponse response = execution.execute(request, body);

            log.info("request method: {}, request URI: {}, request headers: {}, request body: {} \n " +
                            "response status code: {}, response headers: {}, response body: {}",
                    request.getMethod(),
                    request.getURI(),
                    request.getHeaders(),
                    new String(body, Charsets.UTF_8),
                    response.getStatusCode(),
                    response.getHeaders(),
                    new String(ByteStreams.toByteArray(response.getBody()), Charsets.UTF_8));

            return response;
        }
    }

    @Slf4j
    public static class ResponseBodyLoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        @SneakyThrows
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)  {
            ClientHttpResponse response = execution.execute(request, body);

            log.info("response status code: {}, response body: {}",
                    response.getStatusCode(),
                    new String(ByteStreams.toByteArray(response.getBody()), Charsets.UTF_8)
            );

            return response;
        }
    }
}
