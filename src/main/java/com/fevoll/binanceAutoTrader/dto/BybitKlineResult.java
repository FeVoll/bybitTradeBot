package com.fevoll.binanceAutoTrader.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BybitKlineResult {
    @Getter
    @JsonProperty("symbol")
    private String symbol;
    @Getter
    @JsonProperty("category")
    private String category;
    @Getter
    @JsonProperty("list")
    private List<List<String>> list;

}
