package com.gtc.tradinggateway.config.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

/**
 * Smart converter to convert {@link MediaType#APPLICATION_FORM_URLENCODED} to POJO. Adds necessary header.
 */
public class FormHttpMessageToPojoConverter extends AbstractHttpMessageConverter<Object> {

    private static final FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();

    protected final ObjectMapper mapper;
    private final Function<Parameters, Map<String, String>> signer;

    public FormHttpMessageToPojoConverter(ObjectMapper mapper) {
        super(formHttpMessageConverter.getSupportedMediaTypes().toArray(new MediaType[0]));
        this.mapper = mapper;
        this.signer = null;
    }

    public FormHttpMessageToPojoConverter(
            ObjectMapper mapper,
            Function<Parameters, Map<String, String>> signer) {
        super(formHttpMessageConverter.getSupportedMediaTypes().toArray(new MediaType[0]));
        this.mapper = mapper;
        this.signer = signer;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        Map<String, String> vals = formHttpMessageConverter.read(null, inputMessage).toSingleValueMap();
        return mapper.convertValue(vals, clazz);
    }

    @Override
    protected void writeInternal(Object value, HttpOutputMessage outputMessage) {
        pojoSerialize(mapper, value, outputMessage, signer);
    }

    @SneakyThrows
    public static String pojoSerialize(
            ObjectMapper mapper,
            Object value,
            Function<Parameters, Map<String, String>> signer) {
        WrappedHttpOutputMessage outputMessage = new WrappedHttpOutputMessage();
        pojoSerialize(mapper, value, outputMessage, signer);
        return outputMessage.content();
    }

    @SneakyThrows
    private static void pojoSerialize(
            ObjectMapper mapper,
            Object value,
            HttpOutputMessage outputMessage,
            Function<Parameters, Map<String, String>> signer) {
        Map<String, String> asMap = mapper.convertValue(value, new TypeReference<Map<String, String>>() {});

        if (null != signer) {
            WrappedHttpOutputMessage wrap = new WrappedHttpOutputMessage();
            pojoSerialize(mapper, value, wrap, null);
            asMap.putAll(signer.apply(new Parameters(asMap, wrap.content())));
        }

        MultiValueMap<String, String> mvMap = new LinkedMultiValueMap<>();
        asMap.forEach(mvMap::add);
        formHttpMessageConverter.write(mvMap, MediaType.APPLICATION_FORM_URLENCODED, outputMessage);
    }

    @Data
    public static class Parameters {

        private final Map<String, String> asMap;
        private final String asString;
    }

    private static class WrappedHttpOutputMessage implements HttpOutputMessage {

        private ByteArrayOutputStream stream = new ByteArrayOutputStream();
        private HttpHeaders headers = new HttpHeaders();

        @Override
        public OutputStream getBody() {
            return stream;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        private String content() {
            return new String(stream.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
