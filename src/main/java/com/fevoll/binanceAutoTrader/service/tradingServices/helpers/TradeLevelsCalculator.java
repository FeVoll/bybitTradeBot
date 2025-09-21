package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import com.fevoll.binanceAutoTrader.dto.Pivot;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradeLevelsCalculator {

    @Autowired
    Settings settings;

    public double calculateATR(List<List<String>> candles, int period) {
        double atr = 0;
        for (int i = 1; i < period; i++) {
            double high = Double.parseDouble(candles.get(i).get(2));
            double low = Double.parseDouble(candles.get(i).get(3));
            double prevClose = Double.parseDouble(candles.get(i - 1).get(4));
            double trueRange = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            atr += trueRange;
        }
        return atr / period;
    }

    public TradeLevels calculateTradeLevels(List<List<String>> candles, double entryPrice, boolean isLong) {
        int atrPeriod = 14;
        double atr = calculateATR(candles, atrPeriod);

        double sl, tp;

        if (isLong) {
            // Для длинной позиции (покупка)
            if (settings.slType == 1) {
                sl = entryPrice * (1 - settings.slPercent / 100);  // SL по проценту из настроек
            } else {
                sl = entryPrice - settings.slAtr * atr;  // SL по ATR из настроек
                //sl = Math.max(Math.min(sl, entryPrice * 0.99), entryPrice * 0.98);
                sl = Math.min(sl, entryPrice * 0.99);
            }

            if (settings.tpType == 1) {
                tp = entryPrice * (1 + settings.tpPercent / 100);  // TP по проценту из настроек
            } else {
                tp = entryPrice + settings.tpAtr * atr;  // TP по ATR из настроек
                //tp = Math.min(Math.max(tp, entryPrice * 1.012), entryPrice * 1.03);
                tp = Math.max(tp, entryPrice * 1.012);
            }

        } else {
            // Для короткой позиции (продажа)
            if (settings.slType == 1) {
                sl = entryPrice * (1 + settings.slPercent / 100);  // SL по проценту из настроек
            } else {
                sl = entryPrice + settings.slAtr * atr;  // SL по ATR из настроек
                //sl = Math.min(Math.max(sl, entryPrice * 1.01), entryPrice * 1.02);
                sl = Math.max(sl, entryPrice * 1.01);
            }

            if (settings.tpType == 1) {
                tp = entryPrice * (1 - settings.tpPercent / 100);  // TP по проценту из настроек
            } else {
                tp = entryPrice - settings.tpAtr * atr;  // TP по ATR из настроек
                //tp = Math.max(Math.min(tp, entryPrice * 0.988), entryPrice * 0.97);
                tp = Math.min(tp, entryPrice * 0.988);
            }
        }

        return new TradeLevels(sl, tp);
    }
    public TradeLevels calculateTradeLevelsForLevel(List<List<String>> candles, double entryPrice, boolean isLong, Pivot pivot) {
        int nBars = 5;
        int atrPeriod = 14;
        double atr = calculateATR(candles, atrPeriod);

        double sl, tp;

        if (isLong) {
            // Расчёт Take Profit
            if (settings.tpType == 1) {
                tp = entryPrice * (1 + settings.tpPercent / 100); // TP по проценту
            } else if (settings.tpType == 3) {
                tp = pivot.getPivot() + settings.tpAtr * atr; // TP по pivot + ATR
                tp = Math.max(tp, entryPrice * 1.012); // минимальный уровень
            } else {
                tp = entryPrice + settings.tpAtr * atr; // TP по ATR
                tp = Math.max(tp, entryPrice * 1.012); // минимальный уровень
            }

            // Расчёт Stop Loss
            if (settings.slType == 1) {
                sl = entryPrice * (1 - settings.slPercent / 100); // SL по проценту
            } else if (settings.slType == 3) {
                // Новая логика: MIN(мин Low за N баров, entry - ATR)
                double historicalMin = minLow(candles, nBars);
                double atrSl = entryPrice - settings.slAtr * atr;
                sl = Math.min(historicalMin, atrSl);

                // Ограничение: SL не может быть дальше от entryPrice, чем TP (%)
                double tpPerc = getPercentageDifference(entryPrice, tp);
                double slLimitByTp = entryPrice * (1 - Math.abs(tpPerc) / 100 * 0.7);
                sl = Math.max(sl, slLimitByTp); // SL не меньше этого предела

            } else {
                // Стандартный SL по ATR
                sl = entryPrice - settings.slAtr * atr;
                sl = Math.min(sl, entryPrice * 0.99); // ограничение сверху
            }

        } else {
            // Расчёт Take Profit
            if (settings.tpType == 1) {
                tp = entryPrice * (1 - settings.tpPercent / 100); // TP по проценту
            } else if (settings.tpType == 3) {
                tp = pivot.getPivot() - settings.tpAtr * atr; // TP по pivot - ATR
                tp = Math.min(tp, entryPrice * 0.988); // максимальный уровень
            } else {
                tp = entryPrice - settings.tpAtr * atr; // TP по ATR
                tp = Math.min(tp, entryPrice * 0.988); // максимальный уровень
            }

            // Расчёт Stop Loss
            if (settings.slType == 1) {
                sl = entryPrice * (1 + settings.slPercent / 100); // SL по проценту
            } else if (settings.slType == 3) {
                // Новая логика: MAX(макс High за N баров, entry + ATR)
                double historicalMax = maxHigh(candles, nBars);
                double atrSl = entryPrice + settings.slAtr * atr;
                sl = Math.max(historicalMax, atrSl);

                // Ограничение: SL не может быть дальше от entryPrice, чем TP (%)
                double tpPerc = getPercentageDifference(entryPrice, tp);
                double slLimitByTp = entryPrice * (1 + Math.abs(tpPerc) / 100 * 0.7);
                sl = Math.min(sl, slLimitByTp); // SL не больше этого предела

            } else {
                // Стандартный SL по ATR
                sl = entryPrice + settings.slAtr * atr;
                sl = Math.max(sl, entryPrice * 1.01); // ограничение снизу
            }
        }

        return new TradeLevels(sl, tp);
    }
    public TradeLevels calculateTradeLevelsForFalseBreakout(List<List<String>> candles, double entryPrice, boolean isLong, Pivot topPivot) {
        int atrPeriod = 14;
        double atr = calculateATR(candles, atrPeriod);
        double sl, tp;
        if (isLong) {
            // 1) Stop‑loss чуть ниже уровня (pivot – ATR)
            sl = topPivot.getPivot() - atr;

            // 2) TP на 1.5× дальше, чем SL
            double slDistance = entryPrice - sl;          // > 0
            tp = entryPrice + 1.5 * slDistance;
        } else {
            // 1) Stop‑loss чуть выше уровня (pivot + ATR)
            sl = topPivot.getPivot() + atr;

            // 2) TP на 1.5× дальше, чем SL
            double slDistance = sl - entryPrice;          // > 0
            tp = entryPrice - 1.5 * slDistance;
        }

        return new TradeLevels(sl, tp);
    }


    private double minLow(List<List<String>> candles, int nBars) {
        return candles.stream()
                .limit(nBars) // Берём первые (самые свежие) nBars
                .mapToDouble(candle -> Double.parseDouble(candle.get(3))) // low цена
                .min()
                .orElseThrow();
    }

    private double maxHigh(List<List<String>> candles, int nBars) {
        return candles.stream()
                .limit(nBars) // Берём первые (самые свежие) nBars
                .mapToDouble(candle -> Double.parseDouble(candle.get(2))) // high цена
                .max()
                .orElseThrow();
    }

    private double getPercentageDifference(double base, double target) {
        return (target - base) / base * 100;
    }



    // Для хранения уровней TP и SL
        public record TradeLevels(double stopLoss, double takeProfit) {
    }
}
