package com.fevoll.binanceAutoTrader.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BybitKlineResponse {
    @Getter
    @JsonProperty("retCode")
    private int retCode;
    @Getter
    @JsonProperty("retMsg")
    private String retMsg;
    @JsonProperty("result")
    @Getter
    private BybitKlineResult result;
    @Getter
    @JsonProperty("time")
    private long time;

}