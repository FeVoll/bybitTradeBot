package com.fevoll.binanceAutoTrader.service.tradingServices.api;

import com.fevoll.binanceAutoTrader.config.Encryption;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BybitApiService {

    @Autowired
    Encryption encryption;

    @Autowired
    Settings settings;

    @Value("${bybit.url}")
    public String url;

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // Устанавливаем таймауты
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public Call buildTickersRequest(){
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder()
                .url(url+"/v5/market/tickers?category=linear")
                .addHeader("X-BAPI-API-KEY", encryption.API_KEY)
                .addHeader("X-BAPI-RECV-WINDOW", Encryption.RECV_WINDOW)
                .addHeader("Content-Type", "application/json")
                .build();

        return client.newCall(request);
    }

    public Call getCline(String symbol, long start, long end, int limit, String timeframe){
        String requestUrl = String.format(
                url+"/v5/market/kline?category=inverse&symbol=%s&interval="+timeframe+"&start=%d&end=%d&limit="+limit,
                symbol, start, end
        );

        Request request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", "application/json")
                .build();
        return httpClient.newCall(request);
    }

    public Call getOrderBook(String symbol) {
        String requestUrl = String.format(
                "%s/v5/market/orderbook?category=linear&symbol=%s&limit=100",
                url, symbol
        );

        Request request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", "application/json")
                .build();

        return httpClient.newCall(request);
    }


}
