package com.fevoll.binanceAutoTrader.dto;

import lombok.Getter;

public class BybitTickersResponseResult {

    @Getter
    private String symbol;
    private String bid1Price;
    private String bid1Size;
    private String ask1Price;
    private String ask1Size;
    @Getter
    private String lastPrice;
    private String prevPrice24h;
    private String price24hPcnt;
    private String highPrice24h;
    private String lowPrice24h;
    @Getter
    private String turnover24h;
    private String volume24h;
    @Getter
    private String fundingRate;
    @Getter
    private String nextFundingTime;
    private String usdIndexPrice;

    public double getPrice24hPcntAsDouble() {
        try {
            return Double.parseDouble(price24hPcnt);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public double getFundingRateAsDouble() {
        try {
            return Double.parseDouble(fundingRate);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void getFormattedOutput() {
        System.out.printf("Symbol: %s, price: %s , 24h Change: %.2f%%%n", symbol, lastPrice, getPrice24hPcntAsDouble() * 100);
    }

    @Override
    public String toString() {
        return "SymbolInfo{" +
                "symbol='" + symbol + '\'' +
                ", bid1Price='" + bid1Price + '\'' +
                ", bid1Size='" + bid1Size + '\'' +
                ", ask1Price='" + ask1Price + '\'' +
                ", ask1Size='" + ask1Size + '\'' +
                ", lastPrice='" + lastPrice + '\'' +
                ", prevPrice24h='" + prevPrice24h + '\'' +
                ", price24hPcnt='" + price24hPcnt + '\'' +
                ", highPrice24h='" + highPrice24h + '\'' +
                ", lowPrice24h='" + lowPrice24h + '\'' +
                ", turnover24h='" + turnover24h + '\'' +
                ", volume24h='" + volume24h + '\'' +
                ", usdIndexPrice='" + usdIndexPrice + '\'' +
                '}';
    }
}
