package com.fevoll.binanceAutoTrader.dto;

import lombok.Getter;

import java.util.List;

public class BybitTickersResponse {
    @Getter
    private Result result;
    public static class Result {
        @Getter
        private List<BybitTickersResponseResult> list;
    }

}
