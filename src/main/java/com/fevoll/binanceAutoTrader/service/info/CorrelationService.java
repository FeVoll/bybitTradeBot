package com.fevoll.binanceAutoTrader.service.info;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fevoll.binanceAutoTrader.dto.BybitKlineResponse;
import com.fevoll.binanceAutoTrader.dto.BybitKlineResult;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import com.fevoll.binanceAutoTrader.service.tradingServices.api.BybitApiService;
import com.fevoll.binanceAutoTrader.service.tradingServices.helpers.CorrelationIndicator;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CorrelationService {

    private final ObjectMapper objectMapper;

    public CorrelationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    CorrelationIndicator correlationIndicator;

    @Autowired
    BybitApiService apiService;

    @Autowired
    Settings settings;


    public boolean prepareAndCalculateCorrelations(String symbol) throws IOException {

        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(30));

        long start = from.toEpochMilli();
        long end = now.toEpochMilli();

        Response response;
        Response btcResponse;

        try {
            btcResponse = apiService.getCline("BTCUSDT",start,end,500, settings.lowTimeFrame).execute();
            response = apiService.getCline(symbol,start,end,500, settings.lowTimeFrame).execute();
            if (response.isSuccessful() && response.body() != null && btcResponse.isSuccessful() && btcResponse.body() != null ) {
                String btcResponseBody = btcResponse.body().string();
                BybitKlineResponse btcKline = new ObjectMapper().readValue(btcResponseBody, BybitKlineResponse.class);

                String responseBody = response.body().string();
                BybitKlineResponse kline = objectMapper.readValue(responseBody, BybitKlineResponse.class);


                BybitKlineResult result = kline.getResult();
                List<List<String>> candles = result.getList();

                BybitKlineResult btcResult = btcKline.getResult();
                List<List<String>> btcCandles = btcResult.getList();

                List<Double> closes = new ArrayList<>();
                List<Double> btcCloses = new ArrayList<>();
                for (List<String> candle : candles) {
                    closes.add(Double.parseDouble(candle.get(4)));
                }
                for (List<String> candle : btcCandles) {
                    btcCloses.add(Double.parseDouble(candle.get(4)));
                }
                double corr = correlationIndicator.calculateCorrelation(closes, btcCloses, 20);
                System.out.println(symbol + " " + corr);
                return corr < settings.btcCorrValue;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
