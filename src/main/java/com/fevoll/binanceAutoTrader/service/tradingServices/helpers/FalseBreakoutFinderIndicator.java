package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import com.fevoll.binanceAutoTrader.dto.Pivot;
import com.fevoll.binanceAutoTrader.dto.PivotSignalDto;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FalseBreakoutFinderIndicator {


    @Autowired
    Settings settings;

    int confirmationBars = 10;
    int minPivotIndexDistance = 0;
    int recentBarsToCheckCross = 4;

    public PivotSignalDto checkSignal(
            List<Double> closes, List<Double> highs, List<Double> lows, String s) {

        int boLen = Math.min(200, closes.size());
        int lll   = Math.max(boLen, 1);

        double chWidth = (Collections.max(highs.subList(0, lll))
                - Collections.min(lows.subList(0, lll)))
                * settings.thresholdRate;

        List<Pivot> pivotsHi = findLastPivotsHigh(highs, settings.prd).stream()
                .filter(p -> !isHighPivotBroken(p, closes.subList(recentBarsToCheckCross,closes.size())))
                .sorted(Comparator.comparingDouble(Pivot::getPivot).reversed())
                .toList();

        List<Pivot> pivotsLo = findLastPivotsLow(lows, settings.prd).stream()
                .filter(p -> !isLowPivotBroken(p, closes.subList(recentBarsToCheckCross,closes.size())))
                .sorted(Comparator.comparingDouble(Pivot::getPivot))
                .toList();

        // выводим
        double currentPrice = closes.get(0);
        double currentHiPrice = highs.get(0);
        double currentLoPrice = lows.get(0);


        // Считаем касания «вниз» для хайов и «вверх» для лоёв
        Map<Pivot, List<Pivot>> hiTouches = getTouchingPivotsHigh(pivotsHi, chWidth);
        Map<Pivot, List<Pivot>> loTouches = getTouchingPivotsLow(pivotsLo, chWidth);


        for (Pivot father : hiTouches.keySet()) {

            List<Pivot> touches = hiTouches.get(father);
            if (touches.size() < settings.minTests) continue;

            double fatherLvl  = father.getPivot();
            Pivot  extremePv  = getMostDistantTouchingPivot(father, touches);
            double extremeLvl = extremePv.getPivot();


            boolean aCross   = crossedAbove(extremeLvl, highs, recentBarsToCheckCross);
            boolean aReturn  = currentPrice < extremeLvl && currentHiPrice > extremeLvl; //или fatherLvl
            //boolean aClean   = isCleanAbove(fatherLvl, highs,
            //        recentBarsToCheckCross, confirmationBars);

            if (aCross && aReturn)
                return new PivotSignalDto(-1, father, extremePv);

            /*
            boolean bCross   = crossedAbove(extremeLvl, highs, recentBarsToCheckCross)
                    && maxHighLastN(highs, recentBarsToCheckCross) <= fatherLvl;
            boolean bReturn  = currentPrice < extremeLvl;
            //boolean bClean   = isCleanAbove(extremeLvl, highs,
            //        recentBarsToCheckCross, confirmationBars);

            if (bCross && bReturn)
                return new PivotSignalDto(-1, father, extremePv);

             */
        }


        /* ----------  LONG: ложный пробой поддержки  ---------- */
        for (Pivot father : loTouches.keySet()) {

            List<Pivot> touches = loTouches.get(father);
            if (touches.size() < settings.minTests) continue;

            double fatherLvl  = father.getPivot();                     // «отец»
            Pivot  extremePv  = getMostDistantTouchingPivot(father, touches);
            double extremeLvl = extremePv.getPivot();                  // крайний уровень


            boolean aCross  = crossedBelow(extremeLvl, lows, recentBarsToCheckCross); //или fatherLvl
            boolean aReturn = currentPrice > extremeLvl && currentLoPrice < extremeLvl; //или fatherLvl
            //boolean aClean  = isCleanBelow(fatherLvl, lows,
            //        recentBarsToCheckCross, confirmationBars);

            if (aCross && aReturn)
                return new PivotSignalDto(1, father, extremePv);      // long‑signal

            /*
            boolean bCross = crossedBelow(extremeLvl, lows, recentBarsToCheckCross)
                    && minLowLastN(lows, recentBarsToCheckCross) >= fatherLvl;
            boolean bReturn = currentPrice > extremeLvl;
            boolean bClean  = isCleanBelow(extremeLvl, lows,
                    recentBarsToCheckCross, confirmationBars);

            if (bCross && bReturn && bClean)
                return new PivotSignalDto(+1, father, extremePv);      // long‑signal

             */
        }


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


    private boolean crossedAbove(double level, List<Double> highs, int n){
        for (int i = 0; i < n && i < highs.size(); i++)
            if (highs.get(i) > level) return true;
        return false;
    }

    /** true, если с бара №start (включительно) до start+bars не было High > level */
    private boolean isCleanAbove(
            double level, List<Double> highs,
            int start, int bars)
    {
        for (int i = start; i <= bars && i < highs.size(); i++)
            if (highs.get(i) > level) return false;
        return true;
    }

    /** true, если Low за последние n баров < level */
    private boolean crossedBelow(double level, List<Double> lows, int n){
        for (int i = 0; i < n && i < lows.size(); i++)
            if (lows.get(i) < level) return true;
        return false;
    }

    /** true, если с бара №start до start+bars не было Low < level */
    private boolean isCleanBelow(
            double level, List<Double> lows,
            int start, int bars)
    {
        for (int i = start; i <= bars && i < lows.size(); i++)
            if (lows.get(i) < level) return false;
        return true;
    }

    /** минимальный Low за последние n баров */
    private double minLowLastN(List<Double> lows, int n){
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n && i < lows.size(); i++)
            if (lows.get(i) < min) min = lows.get(i);
        return min;
    }
    private double maxHighLastN(List<Double> highs, int n){
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n && i < highs.size(); i++)
            if (highs.get(i) > max) max = highs.get(i);
        return max;
    }



}
