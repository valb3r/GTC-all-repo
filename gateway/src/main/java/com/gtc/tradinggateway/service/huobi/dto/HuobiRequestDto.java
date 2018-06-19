package com.gtc.tradinggateway.service.huobi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gtc.tradinggateway.service.huobi.HuobiEncryptionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
@JsonPropertyOrder({"AccessKeyId", "SignatureMethod", "SignatureVersion", "Timestamp"})
@RequiredArgsConstructor
public class HuobiRequestDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @JsonProperty("AccessKeyId")
    private final String accessKeyId;

    @JsonProperty("SignatureMethod")
    private final String signatureMethod = HuobiEncryptionService.METHOD;

    @JsonProperty("SignatureVersion")
    private final String signatureVersion = "2";

    @JsonProperty("Timestamp")
    private final String timestamp = FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC));
}
