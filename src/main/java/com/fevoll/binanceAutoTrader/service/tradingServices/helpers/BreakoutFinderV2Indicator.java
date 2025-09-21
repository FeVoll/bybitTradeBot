package com.fevoll.binanceAutoTrader.service.tradingServices.helpers;

import com.fevoll.binanceAutoTrader.dto.LowTfSignalDto;
import com.fevoll.binanceAutoTrader.dto.Pivot;
import com.fevoll.binanceAutoTrader.dto.PivotSignalDto;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BreakoutFinderV2Indicator {


    @Autowired
    Settings settings;

    int confirmationBars = 10;
    int minPivotIndexDistance = 0;

    public PivotSignalDto checkSignal(
            List<Double> closes, List<Double> highs, List<Double> lows, String s) {

        int boLen = Math.min(200, closes.size());
        int lll   = Math.max(boLen, 1);

        double chWidthBase = (Collections.max(highs.subList(0, lll))
                - Collections.min(lows.subList(0, lll)));

        double chWidth = chWidthBase * settings.thresholdRate * 2;
        double chWidthBetweenPivots = chWidthBase * settings.thresholdRateBetweenPivots;   // ← будет использоваться

        List<Pivot> pivotsHi = findLastPivotsHigh(highs, settings.prd).stream()
                .filter(p -> !isHighPivotBroken(p, closes))
                .sorted(Comparator.comparingDouble(Pivot::getPivot).reversed())
                .toList();

        List<Pivot> pivotsLo = findLastPivotsLow(lows, settings.prd).stream()
                .filter(p -> !isLowPivotBroken(p, closes))
                .sorted(Comparator.comparingDouble(Pivot::getPivot))
                .toList();

        double currentPrice = closes.get(0);

        // 1) Лонг: убираем уровни, уже пробитые вверх
        if (!pivotsHi.isEmpty()) {
            Pivot topHi = pivotsHi.get(0);
            if (currentPrice > topHi.getPivot()) {
                pivotsHi = List.of();
            }
        }

        // 2) Шорт: убираем уровни, уже пробитые вниз
        if (!pivotsLo.isEmpty()) {
            Pivot lowLo = pivotsLo.get(0);
            if (currentPrice < lowLo.getPivot()) {
                pivotsLo = List.of();
            }
        }

        // 3) Группируем касания
        Map<Pivot, List<Pivot>> hiTouches = getTouchingPivotsHigh(pivotsHi, chWidth);
        Map<Pivot, List<Pivot>> loTouches = getTouchingPivotsLow(pivotsLo, chWidth);

        /* ======== LONG-СЦЕНАРИЙ ======== */
        for (Pivot p : hiTouches.keySet()) {
            List<Pivot> touches = hiTouches.get(p);

            if (touches.size() < settings.minTests) continue;

            /* --- НОВАЯ ПРОВЕРКА (для хай-кластера) --- */
            double maxHi = Math.max(p.getPivot(),
                    touches.stream().mapToDouble(Pivot::getPivot).max().orElse(p.getPivot()));
            double minHi = Math.min(p.getPivot(),
                    touches.stream().mapToDouble(Pivot::getPivot).min().orElse(p.getPivot()));

            if ((maxHi - minHi) > chWidthBetweenPivots) {          // если разброс слишком велик — пропускаем
                continue;
            }
            /* ---------------------------------------- */

            double level = hiTouches.get(p).get(hiTouches.get(p).size()-1).getPivot();
            if (currentPrice >= (level - chWidth * settings.chBoxEntryMultiplier) &&
                    currentPrice <= level) {

                boolean clean = true;
                for (int i = 0; i <= confirmationBars && i < highs.size(); i++) {
                    if (highs.get(i) > level) { clean = false; break; }
                }
                if (clean) {
                    Pivot mostDistant = getMostDistantTouchingPivot(p, touches);
                    return new PivotSignalDto(1, p, mostDistant);
                }
            }
        }

        /* ======== SHORT-СЦЕНАРИЙ ======== */
        for (Pivot p : loTouches.keySet()) {
            List<Pivot> touches = loTouches.get(p);

            if (touches.size() < settings.minTests) continue;

            /* --- НОВАЯ ПРОВЕРКА (для лоу-кластера) --- */
            double maxLo = Math.max(p.getPivot(),
                    touches.stream().mapToDouble(Pivot::getPivot).max().orElse(p.getPivot()));
            double minLo = Math.min(p.getPivot(),
                    touches.stream().mapToDouble(Pivot::getPivot).min().orElse(p.getPivot()));

            if ((maxLo - minLo) > chWidthBetweenPivots) {          // если разброс слишком велик — пропускаем
                continue;
            }
            /* ---------------------------------------- */

            double level = loTouches.get(p).get(loTouches.get(p).size()-1).getPivot();
            if (currentPrice >= level &&
                    currentPrice <= (level + chWidth * settings.chBoxEntryMultiplier)) {

                boolean clean = true;
                for (int i = 0; i <= confirmationBars && i < lows.size(); i++) {
                    if (lows.get(i) < level) { clean = false; break; }
                }
                if (clean) {
                    Pivot mostDistant = getMostDistantTouchingPivot(p, touches);
                    return new PivotSignalDto(-1, p, mostDistant);
                }
            }
        }

        // 4) Нет сигнала
        return new PivotSignalDto(0, null, null);
    }
    public LowTfSignalDto getLowTfSignal(List<Double> closes,
                                         List<Double> highs,
                                         List<Double> lows,
                                         Pivot        firstPivot,
                                         Pivot        lastPivot,
                                         int          mainSignal,
                                         double       lowTFAtr,
                                         double       volumeOsc) {

        if (volumeOsc < settings.volumeOsc) {
            return new LowTfSignalDto(0, 0, null, null);
        }
        double curr = closes.get(0); // текущий close
        double prev = closes.get(1); // предыдущий close

        /* ---------- 1. собираем свежие low-TF пивоты нужного типа ---------- */
        List<Pivot> pivots = (mainSignal == 1)
                ? findLastPivotsHigh(highs, settings.lowTfPrd).stream()
                .filter(p -> !wasHighPivotBrokenEarlier(p, closes, 2))
                .sorted(Comparator.comparingInt(Pivot::getIndex))
                .toList()
                : findLastPivotsLow(lows, settings.lowTfPrd).stream()
                .filter(p -> !wasLowPivotBrokenEarlier(p, closes, 2))
                .sorted(Comparator.comparingInt(Pivot::getIndex))
                .toList();

        /* ---------- 2. вход по пробою любого low-TF пивота ---------- */
        boolean entered = false;
        for (Pivot p : pivots) {
            double level = p.getPivot();
            if (mainSignal == 1 && prev <= level && curr > level) { // LONG-пробой
                entered = true; break;
            }
            if (mainSignal == -1 && prev >= level && curr < level) { // SHORT-пробой
                entered = true; break;
            }
        }

        /* ---------- 3. fallback-вход: цена ≤ 4×ATR от high-TF уровня ---------- */
        if (!entered) {
            double dist  = Math.abs(curr - firstPivot.getPivot());
            boolean near = dist <= 4.0 * lowTFAtr;
            boolean side = (mainSignal == 1 && curr <= firstPivot.getPivot()) ||
                    (mainSignal == -1 && curr >= firstPivot.getPivot());
            entered = near && side;
        }

        if (!entered) {
            return new LowTfSignalDto(0, 0, null, null);
        }

        /* ---------- 3.5. фильтр «без отката» за последние 30 закрытий ---------- */
        int lookback = Math.min(30, closes.size() - 1); // берём столько, сколько есть, максимум 30
        boolean momentumOK = true;
        if (mainSignal == 1) { // LONG: все прошлые закрытия строго ниже текущего
            for (int i = 1; i <= lookback; i++) {
                if (closes.get(i) >= curr) { momentumOK = false; break; }
            }
        } else { // SHORT: все прошлые закрытия строго выше текущего
            for (int i = 1; i <= lookback; i++) {
                if (closes.get(i) <= curr) { momentumOK = false; break; }
            }
        }
        if (!momentumOK) {
            return new LowTfSignalDto(0, 0, null, null);
        }

        /* ---------- 4. рассчитываем STOP-LOSS (пивот с наименьшим index > 0) ---------- */
        Double sl;
        if (mainSignal == 1) {  // LONG ⇒ самый свежий ЛОУ-пивот ниже entry
            sl = findLastPivotsLow(lows.subList(0, Math.min(lows.size(), 60)), settings.lowTfPrd).stream()
                    .filter(p -> p.getIndex() > 0)          // пивот не на текущем баре
                    .filter(p -> p.getPivot() < curr)       // SL должен быть ниже цены входа
                    .min(Comparator.comparingInt(Pivot::getIndex))   // минимальный индекс = ближе всего к 0
                    .map(Pivot::getPivot)
                    .orElseGet(() ->
                            lows.stream().limit(5)              // fallback: минимум последних 5 баров
                                    .min(Double::compare).orElse(null)
                    );
        } else {              // SHORT ⇒ самый свежий ХАЙ-пивот выше entry
            sl = findLastPivotsHigh(highs.subList(0, Math.min(highs.size(), 60)), settings.lowTfPrd).stream()
                    .filter(p -> p.getIndex() > 0)
                    .filter(p -> p.getPivot() > curr)       // SL должен быть выше цены входа
                    .min(Comparator.comparingInt(Pivot::getIndex))
                    .map(Pivot::getPivot)
                    .orElseGet(() ->
                            highs.stream().limit(5)             // fallback: максимум последних 5 баров
                                    .max(Double::compare).orElse(null)
                    );
        }

        if (sl == null) {
            return new LowTfSignalDto(0, 0, null, null);
        }

        /* ---------- 5. TP = lastPivot ± 1.5 × ATR (в сторону сделки) ---------- */
        double tp = (mainSignal == 1)
                ? lastPivot.getPivot() + 1.5 * lowTFAtr  // LONG: выше целевого пивота
                : lastPivot.getPivot() - 1.5 * lowTFAtr; // SHORT: ниже целевого пивота

        double reward = Math.abs(tp - curr);

        double maxRisk = 0.8 * reward;
        double risk    = Math.abs(curr - sl);
        if (risk > maxRisk) {
            if (mainSignal == 1) {
                // LONG: подтянуть SL вверх, но не выше entry
                sl = Math.min(curr, Math.max(sl, curr - maxRisk));
            } else {
                // SHORT: подтянуть SL вниз, но не ниже entry
                sl = Math.max(curr, Math.min(sl, curr + maxRisk));
            }
        }

        /* ---------- 6. результат ---------- */
        return new LowTfSignalDto(mainSignal, curr, sl, tp);
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

    private boolean wasHighPivotBrokenEarlier(Pivot pivot,
                                              List<Double> closes,
                                              int barsAgo) {
        int pivotIdx = pivot.getIndex();
        for (int i = pivotIdx - 1; i >= barsAgo; i--) {   // всё, что старше barsAgo
            if (closes.get(i) > pivot.getPivot()) return true;
        }
        return false;
    }

    private boolean wasLowPivotBrokenEarlier(Pivot pivot,
                                             List<Double> closes,
                                             int barsAgo) {
        int pivotIdx = pivot.getIndex();
        for (int i = pivotIdx - 1; i >= barsAgo; i--) {
            if (closes.get(i) < pivot.getPivot()) return true;
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
