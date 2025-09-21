package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import com.fevoll.binanceAutoTrader.dto.Pivot;
import com.fevoll.binanceAutoTrader.dto.PivotSignalDto;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BreakoutFinderIndicator {


    @Autowired
    Settings settings;

    int confirmationBars = 10;
    int minPivotIndexDistance = 0;

    public PivotSignalDto checkSignal(
            List<Double> closes, List<Double> highs, List<Double> lows, String s) {

        int boLen = Math.min(200, closes.size());
        int lll   = Math.max(boLen, 1);

        double chWidth = (Collections.max(highs.subList(0, lll))
                - Collections.min(lows.subList(0, lll)))
                * settings.thresholdRate;

        List<Pivot> pivotsHi = findLastPivotsHigh(highs, settings.prd).stream()
                .filter(p -> !isHighPivotBroken(p, closes))
                .sorted(Comparator.comparingDouble(Pivot::getPivot).reversed())
                .toList();

        List<Pivot> pivotsLo = findLastPivotsLow(lows, settings.prd).stream()
                .filter(p -> !isLowPivotBroken(p, closes))
                .sorted(Comparator.comparingDouble(Pivot::getPivot))
                .toList();

        // выводим
        double currentPrice = closes.get(0);

        // 1) Лонговый сценарий: следим за верхними пивотами
        if (!pivotsHi.isEmpty()) {
            Pivot topHi = pivotsHi.get(0);          // самый высокий
            if (currentPrice > topHi.getPivot()) {  // цена УЖЕ выше уровня
                pivotsHi = List.of();               // ←обнуляем список
            }
        }

        // 2) Шорт‑сценарий: следим за нижними пивотами
        if (!pivotsLo.isEmpty()) {
            Pivot lowLo = pivotsLo.get(0);          // самый низкий
            if (currentPrice < lowLo.getPivot()) {  // цена УЖЕ ниже уровня
                pivotsLo = List.of();               // ←обнуляем список
            }
        }

        // Считаем касания «вниз» для хайов и «вверх» для лоёв
        Map<Pivot, List<Pivot>> hiTouches = getTouchingPivotsHigh(pivotsHi, chWidth);
        Map<Pivot, List<Pivot>> loTouches = getTouchingPivotsLow(pivotsLo, chWidth);


        for (Pivot p : hiTouches.keySet()) {
            List<Pivot> touches = hiTouches.get(p);

            if (touches.size() < settings.minTests) {
                continue;
            }

            double level = p.getPivot();
            if (currentPrice >= (level - chWidth * settings.chBoxEntryMultiplier) &&
                    currentPrice <= level) {

                boolean clean = true;
                for (int i = 0; i <= confirmationBars && i < highs.size(); i++) {
                    if (highs.get(i) > level) {
                        clean = false;
                        break;
                    }
                }
                if (clean) {
                    Pivot mostDistant = getMostDistantTouchingPivot(p, touches);
                    return new PivotSignalDto(1, p, mostDistant);
                }
            }
        }

        for (Pivot p : loTouches.keySet()) {
            List<Pivot> touches = loTouches.get(p);

            if (touches.size() < settings.minTests) {
                continue;
            }

            double level = p.getPivot();
            if (currentPrice >= level &&
                    currentPrice <= (level + chWidth * settings.chBoxEntryMultiplier)) {

                boolean clean = true;
                for (int i = 0; i <= confirmationBars && i < lows.size(); i++) {
                    if (lows.get(i) < level) {
                        clean = false;
                        break;
                    }
                }
                if (clean) {
                    Pivot mostDistant = getMostDistantTouchingPivot(p, touches);
                    return new PivotSignalDto(-1, p, mostDistant);
                }
            }
        }
        // 3) Иначе — нет сигнала
        return new PivotSignalDto(0, null, null);
    }

    public Map<Pivot, List<Pivot>> getTouchingPivotsHigh(
            List<Pivot> pivots, double chWidth) {

        Map<Pivot, List<Pivot>> touchingPivots = new LinkedHashMap<>();

        for (Pivot p : pivots) {
            List<Pivot> touches = pivots.stream()
                    .filter(other -> other != p)
                    .filter(other ->
                            // другие должны быть ниже, в пределах chWidth
                            other.getPivot() < p.getPivot() &&
                                    (p.getPivot() - other.getPivot()) <= chWidth &&
                                    // индексная дистанция
                                    Math.abs(other.getIndex() - p.getIndex()) >= minPivotIndexDistance
                    )
                    .toList();

            if (!touches.isEmpty()) {
                touchingPivots.put(p, touches);
            }
        }

        return touchingPivots;
    }


    public Map<Pivot, List<Pivot>> getTouchingPivotsLow(
            List<Pivot> pivots, double chWidth) {

        Map<Pivot, List<Pivot>> touchingPivots = new LinkedHashMap<>();

        for (Pivot p : pivots) {
            List<Pivot> touches = pivots.stream()
                    .filter(other -> other != p)
                    .filter(other ->
                            // другие должны быть выше, в пределах chWidth
                            other.getPivot() > p.getPivot() &&
                                    (other.getPivot() - p.getPivot()) <= chWidth
                    )
                    .toList();

            if (!touches.isEmpty()) {
                touchingPivots.put(p, touches);
            }
        }

        return touchingPivots;
    }




    public List<Pivot> findLastPivotsHigh(List<Double> highs, int length) {
        List<Pivot> pivots = new ArrayList<>();

        // Проверяем остальные индексы
        for (int currentIndex = length + 1; currentIndex <= highs.size() - length - 1; currentIndex++) {
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
                if (i < highs.size() && highs.get(i) > pivot) {
                    isPivot = false;
                    break;
                }
            }

            if (isPivot) {
                pivots.add(new Pivot(pivot, currentIndex));
            }
        }

        return pivots;
    }

    public List<Pivot> findLastPivotsLow(List<Double> lows, int length) {
        List<Pivot> pivots = new ArrayList<>();

        // Проверяем остальные индексы
        for (int currentIndex = length + 1; currentIndex <= lows.size() - length - 1; currentIndex++) {
            double pivot = lows.get(currentIndex);
            boolean isPivot = true;

            // Проверяем бары слева
            for (int i = currentIndex - length; i < currentIndex; i++) {
                if (i >= 0 && lows.get(i) < pivot) {
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

            if (isPivot) {
                pivots.add(new Pivot(pivot, currentIndex));
            }
        }

        return pivots;
    }


    private boolean isHighPivotBroken(Pivot pivot, List<Double> closes) {
        int pivotIdx = pivot.getIndex();
        // если пивот на текущем баре — не на чем проверять
        if (pivotIdx <= 0) {
            return false;
        }
        // перебираем бары от (pivotIdx-1) до 0 (самого свежего)
        for (int i = pivotIdx - 1; i >= 1; i--) {
            if (closes.get(i) > pivot.getPivot()) {
                return true;
            }
        }
        return false;
    }

    private boolean isLowPivotBroken(Pivot pivot, List<Double> closes) {
        int pivotIdx = pivot.getIndex();
        // если пивот на текущем баре — не на чем проверять
        if (pivotIdx <= 0) {
            return false;
        }
        // перебираем бары от (pivotIdx-1) до 0 (самого свежего)
        for (int i = pivotIdx - 1; i >= 1; i--) {
            if (closes.get(i) < pivot.getPivot()) {
                return true;
            }
        }
        return false;
    }


    private Pivot getMostDistantTouchingPivot(Pivot mainPivot, List<Pivot> touches) {
        if (touches == null || touches.isEmpty()) return null;

        return touches.stream()
                .max(Comparator.comparingDouble(p -> Math.abs(p.getPivot() - mainPivot.getPivot())))
                .orElse(null);
    }



}
