package com.gtc.rxsupport;

import com.appunite.websocket.rx.object.ObjectParseException;
import com.appunite.websocket.rx.object.ObjectSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@Slf4j
public class JacksonSerializer implements ObjectSerializer {

    private final ObjectMapper objectMapper;

    public JacksonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Nonnull
    @Override
    @SneakyThrows
    public Object serialize(@Nonnull String message) {
        return objectMapper.reader().readTree(message);
    }

    @Nonnull
    @Override
    @SneakyThrows
    public Object serialize(@Nonnull byte[] message) {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(
                             new GZIPInputStream(new ByteArrayInputStream(message))))) {
            return objectMapper.reader().readTree(reader.lines().collect(Collectors.joining()));
        }
    }

    @Nonnull
    @Override
    public byte[] deserializeBinary(@Nonnull Object message) {
        throw new IllegalAccessError("Not implemented");
    }

    @Nonnull
    @Override
    @SneakyThrows
    public String deserializeString(@Nonnull Object message) throws ObjectParseException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
    }

    @Override
    public boolean isBinary(@Nonnull Object message) {
        return false;
    }
}
