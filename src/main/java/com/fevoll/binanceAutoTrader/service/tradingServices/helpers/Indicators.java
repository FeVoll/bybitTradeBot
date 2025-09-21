package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import com.fevoll.binanceAutoTrader.dto.Pivot;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Indicators {

    @Autowired
    Settings settings;

    // Метод для вычисления EMA
    public double calculateEMA(List<Double> prices, int period) {
        if (prices == null || prices.isEmpty() || period <= 0) {
            throw new IllegalArgumentException("Invalid input data.");
        }
        // Рассчитываем множитель для EMA
        double multiplier = 2.0 / (period + 1);

        // Инициализация начального значения EMA (используем последнюю цену как первую)
        double ema = prices.get(prices.size() - 1); // Самая свежая свеча (индекс 0 по логике)

        // Проходим по остальным ценам, начиная с самой старой, и считаем EMA
        for (int i = prices.size() - 2; i >= 0; i--) {
            ema = ((prices.get(i) - ema) * multiplier) + ema;
        }

        return ema;
    }

    public List<Pivot> findLastPivotsHigh(List<Double> highs, int length) {
        int pivotsToFind = 5;
        List<Pivot> pivots = new ArrayList<>();
        for (int currentIndex = length+1 ; currentIndex <= highs.size() - length - 1; currentIndex++) {
            double pivot = highs.get(currentIndex);
            boolean isPivot = true;

            // Проверяем бары слева
            for (int i = currentIndex - length; i < currentIndex; i++) {
                if (i >= 0 && highs.get(i) >= pivot) {
                    isPivot = false;
                    break;
                }
            }

            // Проверяем бары справа
            for (int i = currentIndex + 1; i <= currentIndex + length; i++) {
                if (i < highs.size() && highs.get(i) >= pivot) {
                    isPivot = false;
                    break;
                }
            }

            if (isPivot && pivotsToFind > 0) {
                pivots.add(new Pivot(pivot, currentIndex));
                pivotsToFind -= 1;
            }
            if (pivotsToFind == 0){
                return pivots;
            }
        }
        return pivots;
    }

    public double findLastPivotHigh(List<Double> highs, int length) {
        for (int currentIndex = length+1 ; currentIndex <= highs.size() - length - 1; currentIndex++) {
            double pivot = highs.get(currentIndex);
            boolean isPivot = true;

            // Проверяем бары слева
            for (int i = currentIndex - length; i < currentIndex; i++) {
                if (i >= 0 && highs.get(i) >= pivot) {
                    isPivot = false;
                    break;
                }
            }

            // Проверяем бары справа
            for (int i = currentIndex + 1; i <= currentIndex + length; i++) {
                if (i < highs.size() && highs.get(i) >= pivot) {
                    isPivot = false;
                    break;
                }
            }

            if (isPivot ) {
                return pivot;
            }
        }
        return 0.;
    }

    public double findLastPivotLow(List<Double> lows, int length) {
        for (int currentIndex = length+1; currentIndex <= lows.size() - length - 1; currentIndex++) {
            double pivot = lows.get(currentIndex);
            boolean isPivot = true;

            // Проверяем бары слева
            for (int i = currentIndex - length; i < currentIndex; i++) {
                if (i >= 0 && lows.get(i) <= pivot) {
                    isPivot = false;
                    break;
                }
            }

            // Проверяем бары справа
            for (int i = currentIndex + 1; i <= currentIndex + length; i++) {
                if (i < lows.size() && lows.get(i) <= pivot) {
                    isPivot = false;
                    break;
                }
            }


            if (isPivot){
                return pivot;
            }
        }
        return 0.;
    }

    public List<Pivot> findLastPivotsLow(List<Double> lows, int length) {
        List<Pivot> pivots = new ArrayList<>();
        int pivotsToFind = 5;
        for (int currentIndex = length+1; currentIndex <= lows.size() - length - 1; currentIndex++) {
            double pivot = lows.get(currentIndex);
            boolean isPivot = true;

            // Проверяем бары слева
            for (int i = currentIndex - length; i < currentIndex; i++) {
                if (i >= 0 && lows.get(i) <= pivot) {
                    isPivot = false;
                    break;
                }
            }

            // Проверяем бары справа
            for (int i = currentIndex + 1; i <= currentIndex + length; i++) {
                if (i < lows.size() && lows.get(i) <= pivot) {
                    isPivot = false;
                    break;
                }
            }


            if (isPivot && pivotsToFind > 0) {
                pivots.add(new Pivot(pivot, currentIndex));
                pivotsToFind -= 1;
            }
            if (pivotsToFind == 0){
                return pivots;
            }
        }
        return pivots;
    }

    public double getLastActualPivotHi(List<Double> closes, List<Pivot> pivots){
        List<Pivot> pivotsToRemove = new ArrayList<>();

        // Проходим по всем барам
        for (int i = closes.size() - 1; i >= settings.prd; i--) {
            double close = closes.get(i);

            for (Pivot pivot : pivots) {
                if (close > pivot.getPivot() && pivot.getIndex() > i) { // Цена полностью пробила зону снизу
                    pivotsToRemove.add(pivot); // Зона неактуальна, удаляем
                }
            }

            // Удаляем все неактуальные зоны спроса
            pivots.removeAll(pivotsToRemove);
            pivotsToRemove.clear();
        }
        if (!pivots.isEmpty()){
            return pivots.get(0).getPivot();
        }else {
            return 0;
        }
    }

    public double getLastActualPivotLo(List<Double> closes, List<Pivot> pivots){
        List<Pivot> pivotsToRemove = new ArrayList<>();

        // Проходим по всем барам
        for (int i = closes.size() - 1; i >= settings.prd; i--) {
            double close = closes.get(i);

            // Проверка зон спроса (Demand)
            for (Pivot pivot : pivots) {
                if (close < pivot.getPivot() && pivot.getIndex() > i) { // Цена полностью пробила зону снизу
                    pivotsToRemove.add(pivot); // Зона неактуальна, удаляем
                }
            }

            // Удаляем все неактуальные зоны спроса
            pivots.removeAll(pivotsToRemove);
            pivotsToRemove.clear();
        }
        if (!pivots.isEmpty()){
            return pivots.get(0).getPivot();
        }else {
            return 0;
        }

    }


    public double calculateATR(List<List<String>> candles, int period) {

        final int OPEN_IDX  = 1;
        final int HIGH_IDX  = 2;
        final int LOW_IDX   = 3;
        final int CLOSE_IDX = 4;

        int bars = candles.size();
        if (bars < period + 1) {
            throw new IllegalArgumentException(
                    "Нужно как минимум period+1 (" + (period + 1) + ") баров для ATR");
        }

        /* ---------- 1. True-Range для каждого бара ---------- */
        double[] tr = new double[bars];

        for (int i = bars - 1; i >= 0; i--) {               // идём от самого старого к новому
            double high = Double.parseDouble(candles.get(i).get(HIGH_IDX));
            double low  = Double.parseDouble(candles.get(i).get(LOW_IDX));

            if (i == bars - 1) {                            // самый старый бар: close[1] нет
                tr[i] = high - low;
            } else {
                double prevClose = Double.parseDouble(candles.get(i + 1).get(CLOSE_IDX));
                tr[i] = Math.max(high - low,
                        Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            }
        }

        /* ---------- 2. seed = SMA(TR, period) на самых старых period барах ---------- */
        double sum = 0.0;
        for (int i = bars - period; i < bars; i++) {        // последние по списку = самые старые
            sum += tr[i];
        }
        double atrPrev = sum / period;                      // ATR для бара (bars-period)

        /* ---------- 3. Wilder RMA к более новым барам ---------- */
        for (int i = bars - period - 1; i >= 0; i--) {      // двигаемся к бару-0
            atrPrev = (atrPrev * (period - 1) + tr[i]) / period;
        }

        return atrPrev;   // это ATR для самого свежего бара (index 0)
    }
}
