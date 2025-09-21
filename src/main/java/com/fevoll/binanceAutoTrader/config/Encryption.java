package com.fevoll.binanceAutoTrader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class Encryption {

    //public final static String API_KEY = "8tZtfhZusBqTo9C3zD"; //main
    @Value("${bybit.apiKey}")
    public String API_KEY;

    //public final static String API_SECRET = "D97El3HGPFsAHU8NCa1sQ8zn2GkK3L3rQItu"; //main
    @Value("${bybit.apiSecret}")
    public String API_SECRET;

    public final static String RECV_WINDOW = "5000";

}