package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PumpAdvancedIndicators {
    public double checkVolumeOsc(List<Double> volumes, String s) {
        int longLen = 100;
        if (volumes == null || volumes.size() < longLen) {
            // Нужно как минимум иметь данных не меньше, чем "длинный" период EMA
            return 0;
        }

        // Считаем EMA по всему массиву с нужными периодами
        int shortLen = 50;
        double shortEma = calculateEMA(volumes, shortLen);
        double longEma = calculateEMA(volumes, longLen);

        // Предусмотрим случай, если longEma = 0 (например, объём вообще не поставляется)
        if (longEma == 0.0) {
            return 0.0;
        }

        // Формула: osc = 100 * (shortEMA - longEMA) / longEMA
        double osc = 100.0 * (shortEma - longEma) / longEma;
        return osc;
    }

    private double calculateEMA(List<Double> volumes, int period) {
        // alpha = 2 / (period + 1)
        double alpha = 2.0 / (period + 1);

        // Начальное значение берём из самой старой свечи
        // В нашем соглашении volumes.size()-1 — это самая старая
        double ema = volumes.get(volumes.size() - 1);

        // Итерируемся к новой свече (к индексу 0)
        for (int i = volumes.size() - 2; i >= 0; i--) {
            ema = alpha * volumes.get(i) + (1.0 - alpha) * ema;
        }

        return ema;
    }
}
