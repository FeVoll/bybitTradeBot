package com.fevoll.binanceAutoTrader.service.tradingServices;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Settings {

    //Основные настройки
    public Double orderAmount = 10.0;
    public Double leverage = 10.0;

    public String lowTimeFrame = "5";
    public List<String> allTimeframes = List.of("1", "5", "15", "30", "60", "120", "180", "240", "D");
    public List<String> activeTimeframes = new ArrayList<>(List.of("15", "30", "60", "120", "180", "240"));

    public Integer activeTrades = 3;

    public boolean volumeWatchList = true;
    public Integer watchlistVolume = 15000000;
    public Integer watchlistVolumeCount = 40;

    //BTC Corr
    public boolean checkBTCCorr = true;
    public Integer btcCorrEntries = 3; //for Sentinel
    public double btcCorrValue = 0.85;

    //Sentinel
    public Double sidewaySens = 8.0;
    public double superTrendSens = 5;

    public int ema = 50;

    //SL TP
    public int tpType = 3; // 1 - фикс процент/ 2 - атр / 3 - смарт уровень
    public int slType = 3;

    public double tpPercent = 0.8;
    public double slPercent = 1;

    public double tpAtr = 2;
    public double slAtr = 1.5;

    //Safe Mod

    public boolean enableSafeMode = true;
    public boolean enableSafeTrailingSL = true;
    public double safeModePivotPercent = 75.0;

    //Breaks

    public int prd = 30; // Период пивота
    public int lowTfPrd = 5; // Период пивота
    public double thresholdRate = 0.08; // Пороговая ширина прорыва
    public double thresholdRateBetweenPivots = 0.06; // Пороговая ширина прорыва
    public int minTests = 1; // Минимальное количество тестов для прорыва
    public double chBoxEntryMultiplier = 1;
    public int volumeOsc = 20;

    public int atrMultiplier = 4;

}
