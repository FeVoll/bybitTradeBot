package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CorrelationIndicator {

    // Метод для расчета корреляции
    public double calculateCorrelation(List<Double> symbolPrices, List<Double> btcPrices, int lookbackPeriod) {
        if (symbolPrices.size() < lookbackPeriod || btcPrices.size() < lookbackPeriod) {
            throw new IllegalArgumentException("Not enough data for the given lookback period");
        }

        // Используем только последние N (lookbackPeriod) значений
        List<Double> symbolSubset = symbolPrices.subList(0, lookbackPeriod);
        List<Double> btcSubset = btcPrices.subList(0, lookbackPeriod);

        double meanSymbol = calculateMean(symbolSubset);
        double meanBtc = calculateMean(btcSubset);

        double numerator = 0.0;
        double sumSymbolSquaredDiffs = 0.0;
        double sumBtcSquaredDiffs = 0.0;

        for (int i = 0; i < lookbackPeriod; i++) {
            double symbolDiff = symbolSubset.get(i) - meanSymbol;
            double btcDiff = btcSubset.get(i) - meanBtc;

            numerator += symbolDiff * btcDiff;
            sumSymbolSquaredDiffs += Math.pow(symbolDiff, 2);
            sumBtcSquaredDiffs += Math.pow(btcDiff, 2);
        }

        double denominator = Math.sqrt(sumSymbolSquaredDiffs * sumBtcSquaredDiffs);

        return denominator == 0 ? 0 : numerator / denominator;
    }

    // Вспомогательный метод для расчета среднего значения
    private static double calculateMean(List<Double> data) {
        double sum = 0.0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.size();
    }
}
