package com.fevoll.binanceAutoTrader.service.info;

import com.fevoll.binanceAutoTrader.dto.BybitTickersResponse;
import com.fevoll.binanceAutoTrader.dto.BybitTickersResponseResult;
import com.fevoll.binanceAutoTrader.service.tradingServices.api.BybitApiService;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import com.google.gson.Gson;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class BybitTickersInfoService {

    @Autowired
    BybitApiService apiService;

    @Autowired
    Settings settings;
    //Получение инфы о свечах
    public List<BybitTickersResponseResult> actualTickersInfo() {

        try {
            Response response = apiService.buildTickersRequest().execute();
            assert response.body() != null;

            Gson gson = new Gson();
            BybitTickersResponse apiResponse = gson.fromJson(response.body().string(), BybitTickersResponse.class);

            List<BybitTickersResponseResult> symbols = apiResponse.getResult().getList();

            return filterByUSDTTopMoversAndVolume(symbols);
            //return filterByUSDT(symbols);

        }catch (IOException e){
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<BybitTickersResponseResult> getAllTickers() {

        try {
            Response response = apiService.buildTickersRequest().execute();
            assert response.body() != null;

            Gson gson = new Gson();
            BybitTickersResponse apiResponse = gson.fromJson(response.body().string(), BybitTickersResponse.class);

            List<BybitTickersResponseResult> symbols = apiResponse.getResult().getList();

            return filterByUSDTAndTopVolume(symbols);
            //return filterByUSDT(symbols);

        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public BybitTickersResponseResult btcInfo() throws IOException {
        Response response = apiService.buildTickersRequest().execute();
        assert response.body() != null;

        Gson gson = new Gson();
        BybitTickersResponse apiResponse = gson.fromJson(response.body().string(), BybitTickersResponse.class);

        List<BybitTickersResponseResult> symbols = apiResponse.getResult().getList();

        return symbols.get(0);
    }

    //Фильтры

    private List<BybitTickersResponseResult> filterByUSDT(List<BybitTickersResponseResult> symbols){
        return symbols.stream()
                .filter(symbol -> symbol.getSymbol().endsWith("USDT")).toList();
    }

    private List<BybitTickersResponseResult> filterByUSDTAndVolume(List<BybitTickersResponseResult> symbols){
        return symbols.stream()
                .filter(symbol -> symbol.getSymbol().endsWith("USDT"))
                .filter(symbol -> Double.parseDouble(symbol.getTurnover24h()) > settings.watchlistVolume)
                .sorted(Comparator.comparingDouble(symbol -> -Double.parseDouble(symbol.getTurnover24h())))
                .limit(settings.watchlistVolumeCount)
                .toList();
    }

    private List<BybitTickersResponseResult> filterByUSDTAndTopChanges(
            List<BybitTickersResponseResult> symbols, Integer volumeThreshold) {

        List<BybitTickersResponseResult> usdtSymbols = symbols.stream()
                .filter(symbol -> symbol.getSymbol().endsWith("USDT"))
                .toList();

        List<BybitTickersResponseResult> topGrowth = usdtSymbols.stream()
                .sorted(Comparator.comparing(BybitTickersResponseResult::getPrice24hPcntAsDouble)
                        .reversed())
                .limit(35)
                .toList();

        List<BybitTickersResponseResult> topDecline = usdtSymbols.stream()
                .sorted(Comparator.comparing(BybitTickersResponseResult::getPrice24hPcntAsDouble))
                .limit(35)
                .toList();

        Set<BybitTickersResponseResult> resultSet = new HashSet<>();
        resultSet.addAll(topGrowth);
        resultSet.addAll(topDecline);

        return new ArrayList<>(resultSet);
    }

    private List<BybitTickersResponseResult> filterByUSDTAndTopGrowth(
            List<BybitTickersResponseResult> symbols) {

        List<BybitTickersResponseResult> usdtSymbols = symbols.stream()
                .filter(symbol -> symbol.getSymbol().endsWith("USDT"))
                .filter(symbol -> {
                    double priceChange = symbol.getPrice24hPcntAsDouble();
                    return (priceChange > 0.1);
                })
                .toList();

        List<BybitTickersResponseResult> topGrowth = usdtSymbols.stream()
                .sorted(Comparator.comparing(BybitTickersResponseResult::getPrice24hPcntAsDouble)
                        .reversed())
                .limit(70)
                .toList();


        Set<BybitTickersResponseResult> resultSet = new HashSet<>();
        resultSet.addAll(topGrowth);

        return new ArrayList<>(resultSet);
    }

    private List<BybitTickersResponseResult> filterByUSDTAndTopVolume(
            List<BybitTickersResponseResult> symbols) {

        return symbols.stream()
                .filter(symbol -> symbol.getSymbol().endsWith("USDT"))
                .sorted(Comparator.comparingDouble(symbol -> -Double.parseDouble(symbol.getTurnover24h())))
                .limit(settings.watchlistVolumeCount)
                .toList();
    }

    private List<BybitTickersResponseResult> filterByUSDTTopMoversAndVolume(
            List<BybitTickersResponseResult> symbols) {
        return symbols.stream()
                // 1. Только USDT-пары
                .filter(symbol -> symbol.getSymbol().endsWith("USDT"))
                // 2. Минимальный объём
                .filter(symbol -> Double.parseDouble(symbol.getTurnover24h())
                        > settings.watchlistVolume)
                // 3. Минимальное изменение цены (абсолютное) > 0.1 (10%)
                .filter(s -> Math.abs(s.getPrice24hPcntAsDouble()) > 0.05)
                // 4. Сортировка по абсолютному изменению, по убыванию
                .sorted(Comparator.comparingDouble(
                        (BybitTickersResponseResult s) ->
                                -Math.abs(s.getPrice24hPcntAsDouble())
                ))
                // 5. Лимит результатов
                .limit(settings.watchlistVolumeCount)
                .toList();
    }

}
