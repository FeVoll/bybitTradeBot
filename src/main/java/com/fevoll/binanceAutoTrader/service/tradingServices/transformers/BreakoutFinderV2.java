package com.fevoll.binanceAutoTrader.service.tradingServices.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fevoll.binanceAutoTrader.dto.*;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import com.fevoll.binanceAutoTrader.service.tradingServices.api.ByBitBasicTradingService;
import com.fevoll.binanceAutoTrader.service.tradingServices.api.BybitApiService;
import com.fevoll.binanceAutoTrader.service.tradingServices.helpers.*;
import com.fevoll.binanceAutoTrader.service.tradingServices.openPositionMonitoring.OpenPositionsMonitoringService;
import com.fevoll.binanceAutoTrader.telegram.Bot;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class BreakoutFinderV2 {

    @Autowired
    Bot bot;
    @Autowired
    OpenOrderTransformer openPositions;
    @Autowired
    ByBitBasicTradingService tradingService;
    @Autowired
    TradeLevelsCalculator levelsCalculator;
    @Autowired
    Indicators indicators;
    @Autowired
    Settings settings;
    @Autowired
    BybitApiService bybitApiService;
    @Autowired
    PumpAdvancedIndicators priceAndVolumeIndicator;

    @Autowired
    BreakoutFinderV2Indicator breakoutFinderIndicator;

    @Autowired
    OpenPositionsMonitoringService monitoringService;

    public void run(BybitKlineResponse response, String timeframe) throws TelegramApiException {
        BybitKlineResult result = response.getResult();
        List<List<String>> candles = result.getList();

        String symbol = result.getSymbol();

        List<Double> closingPrices = new ArrayList<>();
        List<Double> highPrices = new ArrayList<>();
        List<Double> lowPrices = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();
        List<Double> opens = new ArrayList<>();
        for (List<String> candle : candles) {
            closingPrices.add(Double.parseDouble(candle.get(4)));
            highPrices.add(Double.parseDouble(candle.get(2)));
            lowPrices.add(Double.parseDouble(candle.get(3)));
            volumes.add(Double.parseDouble(candle.get(5)));
            opens.add(Double.parseDouble(candle.get(1)));
        }

        double ema = indicators.calculateEMA(closingPrices, settings.ema);

        PivotSignalDto signal =  breakoutFinderIndicator.checkSignal(closingPrices,highPrices,lowPrices, symbol);

        int mainTfSignal = 0;
        if (1 == signal.getSignal() && ema < closingPrices.get(0)) { //&& volumeOsc >= settings.volumeOsc
            mainTfSignal = 1;
        } else if (-1 == signal.getSignal() && ema > closingPrices.get(0) && !"15".equals(timeframe) && !"5".equals(timeframe)) { //&& volumeOsc >= settings.volumeOsc
            mainTfSignal = -1;
        }

        if (mainTfSignal != 0 && !openPositions.hasRecentTrade(symbol, 12)){//повторные трейды

            //есть сигнал? -> получаем лоу тф, высчитываем там атр, получаем точку входа (лоу тф сигнал), высчитываем тп и сл

            List<List<String>> lowTFCandles = getLowTimeFrameCandles(symbol, settings.lowTimeFrame);

            List<Double> lowTFclosingPrices = new ArrayList<>();
            List<Double> lowTFhighPrices = new ArrayList<>();
            List<Double> lowTFlowPrices = new ArrayList<>();
            List<Double> lowTFvolumes = new ArrayList<>();
            List<Double> lowTFopens = new ArrayList<>();
            for (List<String> candle : lowTFCandles) {
                lowTFclosingPrices.add(Double.parseDouble(candle.get(4)));
                lowTFhighPrices.add(Double.parseDouble(candle.get(2)));
                lowTFlowPrices.add(Double.parseDouble(candle.get(3)));
                lowTFvolumes.add(Double.parseDouble(candle.get(5)));
                lowTFopens.add(Double.parseDouble(candle.get(1)));
            }

            double lowTFAtr = indicators.calculateATR(lowTFCandles, 14);
            double volumeOsc = priceAndVolumeIndicator.checkVolumeOsc(volumes, symbol);
            LowTfSignalDto lowTfSignal = breakoutFinderIndicator.getLowTfSignal(lowTFclosingPrices, lowTFhighPrices,lowTFlowPrices,
                    signal.getFirstPivot(), signal.getPivot(), signal.getSignal(), lowTFAtr, volumeOsc);


            //Открываем трейд
            if (!openPositions.getSymbols().containsKey(symbol) && openPositions.getSymbols().size() < settings.activeTrades) {

                double breakevenPoint = getBreakevenPoint(closingPrices, signal, mainTfSignal);

                if (1 == mainTfSignal && 1 == lowTfSignal.side && opens.get(0) < closingPrices.get(0)) { //BUY
                    tradingService.createOrder(symbol, "Buy", closingPrices.get(0),
                            String.valueOf(lowTfSignal.takeProfit), String.valueOf(lowTfSignal.stopLoss));

                    openPositions.addSymbol(symbol,"Buy");
                    openPositions.addTrade(symbol);

                    monitoringService.addBe(symbol, breakevenPoint, signal.getPivot().getPivot());

                    bot.sendDetailedBFNotification(symbol, "Buy", String.valueOf(closingPrices.get(0)),
                            signal.getPivot().getPivot(),
                            lowTfSignal.takeProfit, lowTfSignal.stopLoss, timeframe, volumeOsc);

                } else if (-1 == mainTfSignal && -1 == lowTfSignal.side && opens.get(0) > closingPrices.get(0)) { //SELL

                    tradingService.createOrder(symbol, "Sell", closingPrices.get(0),
                            String.valueOf(lowTfSignal.takeProfit), String.valueOf(lowTfSignal.stopLoss));

                    openPositions.addSymbol(symbol,"Sell");
                    openPositions.addTrade(symbol);

                    monitoringService.addBe(symbol, breakevenPoint, signal.getPivot().getPivot());

                    bot.sendDetailedBFNotification(symbol, "Sell",String.valueOf(closingPrices.get(0)),
                            signal.getPivot().getPivot(),
                            lowTfSignal.takeProfit, lowTfSignal.stopLoss, timeframe, volumeOsc);
                }
            }

        }
    }

    private double getBreakevenPoint(List<Double> closingPrices, PivotSignalDto signal, Integer currentSignal) {
        double entryPrice = closingPrices.get(0);
        double pivotPrice = signal.getFirstPivot().getPivot();
        double breakevenPoint;

        // Проценты для расчета BE (настраиваемое значение)
        double bePercentage = settings.safeModePivotPercent;
        if (1 == currentSignal) {
            // Лонг: рассчитываем BE как процент от расстояния (входа) до пивота
            double diff = pivotPrice - entryPrice;
            breakevenPoint = entryPrice + (diff * (bePercentage / 100.0));
        } else { // Signal DOWN
            // Шорт: рассчитываем BE как процент от расстояния до пивота
            double diff = entryPrice - pivotPrice;
            breakevenPoint = entryPrice - (diff * (bePercentage / 100.0));
        }

        return breakevenPoint;
    }

    public List<List<String>> getLowTimeFrameCandles(String symbol, String timeframe){

        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(1000));

        long start = from.toEpochMilli();
        long end = now.toEpochMilli();
        try (Response response = bybitApiService.getCline(symbol, start, end, 100, timeframe).execute()) {
            if (response.isSuccessful() && response.body() != null) {

                String responseBody = response.body().string();
                //responseBody = readFromFile("src/main/resources/lowCline.txt");
                BybitKlineResponse klineResponse = new ObjectMapper().readValue(responseBody, BybitKlineResponse.class);
                BybitKlineResult result = klineResponse.getResult();
                return result.getList();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    private String readFromFile(String filePath) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
    }

}
