package com.fevoll.binanceAutoTrader.service.tradingServices.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fevoll.binanceAutoTrader.dto.BybitKlineResponse;
import com.fevoll.binanceAutoTrader.dto.BybitKlineResult;
import com.fevoll.binanceAutoTrader.dto.Pivot;
import com.fevoll.binanceAutoTrader.dto.PivotSignalDto;
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
public class FalseBreakoutFinder {

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
    FalseBreakoutFinderIndicator falseBreakoutFinderIndicator;

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

        PivotSignalDto signal =  falseBreakoutFinderIndicator.checkSignal(closingPrices,highPrices,lowPrices, symbol);

        int mainTfSignal = signal.getSignal();
        /*
        if (1 == signal.getSignal()) { //&& volumeOsc >= settings.volumeOsc && ema > closingPrices.get(0)
            mainTfSignal = 1;
        } else if (-1 == signal.getSignal()) { //&& volumeOsc >= settings.volumeOsc && ema < closingPrices.get(0)
            mainTfSignal = -1;
        }
         */

        if (mainTfSignal != 0 && !openPositions.hasRecentTrade(symbol, 4)){

            //есть сигнал? -> получаем лоу тф, высчитываем там атр, получаем точку входа (лоу тф сигнал), высчитываем тп и сл

            List<List<String>> lowTFCandles = getLowTimeFrameCandles(symbol, "5");
            double lowTFAtr = indicators.calculateATR(lowTFCandles, 14);
            double volumeOsc = priceAndVolumeIndicator.checkVolumeOsc(volumes, symbol);
            //int lowTfSignal = getLowTfSignal(closingPrices.get(0), signal.getFirstPivot(), signal.getPivot(), lowTFAtr, volumeOsc);

            if (!openPositions.getSymbols().containsKey(symbol) && openPositions.getSymbols().size() < settings.activeTrades) {

                //double breakevenPoint = getBreakevenPoint(closingPrices, signal, mainTfSignal);

                if (1 == mainTfSignal) { //BUY && 1 == lowTfSignal && opens.get(0) < closingPrices.get(0)
                    TradeLevelsCalculator.TradeLevels levels = levelsCalculator.calculateTradeLevelsForFalseBreakout(lowTFCandles,
                            closingPrices.get(0), true, signal.getPivot());

                    tradingService.createOrder(symbol, "Buy", closingPrices.get(0),
                            String.valueOf(levels.takeProfit()), String.valueOf(levels.stopLoss()));
                    openPositions.addSymbol(symbol,"Buy");
                    openPositions.addTrade(symbol);



                    //monitoringService.addBe(symbol, breakevenPoint, signal.getPivot().getPivot());
                    bot.sendDetailedBFNotification(symbol, "Buy", String.valueOf(closingPrices.get(0)),
                            signal.getPivot().getPivot(),
                            levels.takeProfit(), levels.stopLoss(), timeframe, volumeOsc);

                } else if (-1 == mainTfSignal) { //SELL && -1 == lowTfSignal && opens.get(0) > closingPrices.get(0)
                    TradeLevelsCalculator.TradeLevels levels = levelsCalculator.calculateTradeLevelsForFalseBreakout(lowTFCandles,
                            closingPrices.get(0), false,signal.getPivot());

                    tradingService.createOrder(symbol, "Sell", closingPrices.get(0),
                            String.valueOf(levels.takeProfit()), String.valueOf(levels.stopLoss()));
                    openPositions.addSymbol(symbol,"Sell");
                    openPositions.addTrade(symbol);
                    //monitoringService.addBe(symbol, breakevenPoint, signal.getPivot().getPivot());
                    bot.sendDetailedBFNotification(symbol, "Sell",String.valueOf(closingPrices.get(0)),
                            signal.getPivot().getPivot(),
                            levels.takeProfit(), levels.stopLoss(), timeframe, volumeOsc);
                }
            }

        }
    }

    private double getBreakevenPoint(List<Double> closingPrices, PivotSignalDto signal, Integer currentSignal) {
        double entryPrice = closingPrices.get(0);
        double pivotPrice = signal.getPivot().getPivot();
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
                //responseBody = "{\"retCode\":0,\"retMsg\":\"OK\",\"result\":{\"symbol\":\"MOODENGUSDT\",\"category\":\"inverse\",\"list\":[[\"1749757200000\",\"0.1771\",\"0.17822\",\"0.17707\",\"0.17734\",\"2875743\",\"510625.88467\"],[\"1749756900000\",\"0.18014\",\"0.18017\",\"0.17645\",\"0.1771\",\"7679348\",\"1368743.56247\"],[\"1749756600000\",\"0.18082\",\"0.18082\",\"0.18004\",\"0.18014\",\"860970\",\"155311.7093\"],[\"1749756300000\",\"0.18128\",\"0.18154\",\"0.18071\",\"0.18082\",\"710348\",\"128593.58946\"],[\"1749756000000\",\"0.18107\",\"0.18129\",\"0.18084\",\"0.18128\",\"847732\",\"153546.1247\"],[\"1749755700000\",\"0.181\",\"0.18254\",\"0.18093\",\"0.18107\",\"3875162\",\"704338.36202\"],[\"1749755400000\",\"0.18093\",\"0.1813\",\"0.18053\",\"0.181\",\"691409\",\"125035.67472\"],[\"1749755100000\",\"0.18068\",\"0.18117\",\"0.18029\",\"0.18093\",\"1014011\",\"183381.27559\"],[\"1749754800000\",\"0.18069\",\"0.18113\",\"0.17946\",\"0.18068\",\"2752989\",\"496007.36285\"],[\"1749754500000\",\"0.18047\",\"0.18113\",\"0.18028\",\"0.18069\",\"695011\",\"125637.38832\"],[\"1749754200000\",\"0.17853\",\"0.18066\",\"0.17851\",\"0.18047\",\"1617872\",\"290793.21734\"],[\"1749753900000\",\"0.17878\",\"0.17912\",\"0.17772\",\"0.17853\",\"3262057\",\"581558.95372\"],[\"1749753600000\",\"0.18034\",\"0.18039\",\"0.17868\",\"0.17878\",\"1678958\",\"301724.91814\"],[\"1749753300000\",\"0.18014\",\"0.18063\",\"0.18013\",\"0.18034\",\"903753\",\"163031.10562\"],[\"1749753000000\",\"0.17947\",\"0.18026\",\"0.17843\",\"0.18014\",\"1659353\",\"297071.98577\"],[\"1749752700000\",\"0.17977\",\"0.17984\",\"0.1785\",\"0.17947\",\"4330303\",\"775962.7576\"],[\"1749752400000\",\"0.18026\",\"0.18054\",\"0.17965\",\"0.17977\",\"1326385\",\"238800.87577\"],[\"1749752100000\",\"0.18078\",\"0.18106\",\"0.17976\",\"0.18026\",\"1192659\",\"215044.35442\"],[\"1749751800000\",\"0.18091\",\"0.18151\",\"0.18078\",\"0.18078\",\"852283\",\"154273.72201\"],[\"1749751500000\",\"0.18256\",\"0.18256\",\"0.18083\",\"0.18091\",\"2144856\",\"389076.51687\"],[\"1749751200000\",\"0.18244\",\"0.18295\",\"0.18186\",\"0.18256\",\"1765907\",\"321867.91304\"],[\"1749750900000\",\"0.18326\",\"0.18326\",\"0.1822\",\"0.18244\",\"1201443\",\"219389.27569\"],[\"1749750600000\",\"0.18437\",\"0.18443\",\"0.18293\",\"0.18326\",\"2605922\",\"478199.12831\"],[\"1749750300000\",\"0.18428\",\"0.18482\",\"0.18428\",\"0.18437\",\"390668\",\"72077.66905\"],[\"1749750000000\",\"0.18378\",\"0.18452\",\"0.18374\",\"0.18428\",\"917741\",\"169168.78658\"],[\"1749749700000\",\"0.18378\",\"0.18425\",\"0.18324\",\"0.18378\",\"1191417\",\"219124.13275\"],[\"1749749400000\",\"0.18404\",\"0.18451\",\"0.18352\",\"0.18378\",\"541295\",\"99551.6919\"],[\"1749749100000\",\"0.18389\",\"0.18471\",\"0.18388\",\"0.18404\",\"1724778\",\"317785.37463\"],[\"1749748800000\",\"0.18354\",\"0.18432\",\"0.18345\",\"0.18389\",\"946510\",\"174124.73079\"],[\"1749748500000\",\"0.18233\",\"0.18365\",\"0.18228\",\"0.18354\",\"2271743\",\"415734.23012\"],[\"1749748200000\",\"0.18245\",\"0.18291\",\"0.18233\",\"0.18233\",\"472013\",\"86197.14172\"],[\"1749747900000\",\"0.18248\",\"0.1829\",\"0.18231\",\"0.18245\",\"469244\",\"85701.17634\"],[\"1749747600000\",\"0.18175\",\"0.18249\",\"0.18166\",\"0.18248\",\"950013\",\"172905.38288\"],[\"1749747300000\",\"0.18183\",\"0.18238\",\"0.18153\",\"0.18175\",\"1306524\",\"237871.40482\"],[\"1749747000000\",\"0.18132\",\"0.18213\",\"0.18084\",\"0.18183\",\"1552266\",\"281554.94293\"],[\"1749746700000\",\"0.182\",\"0.18233\",\"0.1811\",\"0.18132\",\"1282203\",\"232659.18013\"],[\"1749746400000\",\"0.18152\",\"0.18227\",\"0.18121\",\"0.182\",\"910215\",\"165479.00176\"],[\"1749746100000\",\"0.18198\",\"0.18242\",\"0.1809\",\"0.18152\",\"2278202\",\"413380.38928\"],[\"1749745800000\",\"0.18362\",\"0.18364\",\"0.18196\",\"0.18198\",\"1888446\",\"344497.89148\"],[\"1749745500000\",\"0.18326\",\"0.18419\",\"0.18277\",\"0.18362\",\"1615511\",\"296632.56495\"],[\"1749745200000\",\"0.1825\",\"0.18326\",\"0.18231\",\"0.18326\",\"1062811\",\"194386.49714\"],[\"1749744900000\",\"0.18101\",\"0.18252\",\"0.18085\",\"0.1825\",\"2019627\",\"367090.04302\"],[\"1749744600000\",\"0.18152\",\"0.18172\",\"0.18074\",\"0.18101\",\"1476055\",\"267566.79088\"],[\"1749744300000\",\"0.18012\",\"0.18184\",\"0.17988\",\"0.18152\",\"3074910\",\"556810.11239\"],[\"1749744000000\",\"0.18072\",\"0.18116\",\"0.18005\",\"0.18012\",\"2005228\",\"362130.36803\"],[\"1749743700000\",\"0.17992\",\"0.18108\",\"0.17944\",\"0.18072\",\"3094511\",\"557697.49871\"],[\"1749743400000\",\"0.18047\",\"0.18114\",\"0.17957\",\"0.17992\",\"5194715\",\"936483.0712\"],[\"1749743100000\",\"0.18318\",\"0.18318\",\"0.18016\",\"0.18047\",\"6889629\",\"1249344.49454\"],[\"1749742800000\",\"0.18327\",\"0.18373\",\"0.18305\",\"0.18318\",\"573406\",\"105125.43198\"],[\"1749742500000\",\"0.18327\",\"0.18387\",\"0.18302\",\"0.18327\",\"1052732\",\"193050.19391\"],[\"1749742200000\",\"0.18311\",\"0.18347\",\"0.18234\",\"0.18327\",\"1625523\",\"297425.3507\"],[\"1749741900000\",\"0.1837\",\"0.18411\",\"0.18277\",\"0.18311\",\"1846326\",\"338522.34777\"],[\"1749741600000\",\"0.18478\",\"0.18481\",\"0.18287\",\"0.1837\",\"2320680\",\"426772.51379\"],[\"1749741300000\",\"0.18524\",\"0.18594\",\"0.18424\",\"0.18478\",\"1410576\",\"260695.27761\"],[\"1749741000000\",\"0.18583\",\"0.18627\",\"0.18515\",\"0.18524\",\"809167\",\"150232.48909\"],[\"1749740700000\",\"0.18573\",\"0.18634\",\"0.18558\",\"0.18583\",\"794702\",\"147818.32179\"],[\"1749740400000\",\"0.18495\",\"0.1859\",\"0.18482\",\"0.18573\",\"1153328\",\"213758.61138\"],[\"1749740100000\",\"0.18595\",\"0.18629\",\"0.18463\",\"0.18495\",\"1699565\",\"314931.09602\"],[\"1749739800000\",\"0.18538\",\"0.18683\",\"0.1852\",\"0.18595\",\"2477354\",\"460663.43973\"],[\"1749739500000\",\"0.18472\",\"0.18555\",\"0.18438\",\"0.18538\",\"1387060\",\"256487.11695\"],[\"1749739200000\",\"0.18468\",\"0.18503\",\"0.18443\",\"0.18472\",\"1676784\",\"309690.84137\"],[\"1749738900000\",\"0.18388\",\"0.18493\",\"0.18384\",\"0.18468\",\"1292359\",\"238421.13888\"],[\"1749738600000\",\"0.18334\",\"0.18402\",\"0.18323\",\"0.18388\",\"867196\",\"159307.59339\"],[\"1749738300000\",\"0.18395\",\"0.18419\",\"0.18324\",\"0.18334\",\"1333270\",\"244895.19121\"],[\"1749738000000\",\"0.18366\",\"0.18395\",\"0.18312\",\"0.18395\",\"1021914\",\"187531.0634\"],[\"1749737700000\",\"0.183\",\"0.18421\",\"0.18261\",\"0.18366\",\"1845756\",\"338801.11023\"],[\"1749737400000\",\"0.18214\",\"0.1832\",\"0.18213\",\"0.183\",\"1117916\",\"204376.82202\"],[\"1749737100000\",\"0.18198\",\"0.1824\",\"0.18143\",\"0.18214\",\"924636\",\"168233.98739\"],[\"1749736800000\",\"0.18224\",\"0.18236\",\"0.18188\",\"0.18198\",\"678498\",\"123593.72833\"],[\"1749736500000\",\"0.18248\",\"0.18249\",\"0.18173\",\"0.18224\",\"836971\",\"152399.1073\"],[\"1749736200000\",\"0.18228\",\"0.18324\",\"0.18214\",\"0.18248\",\"922091\",\"168484.98897\"],[\"1749735900000\",\"0.18216\",\"0.1825\",\"0.18178\",\"0.18228\",\"1119887\",\"203833.48629\"],[\"1749735600000\",\"0.18253\",\"0.18254\",\"0.18193\",\"0.18216\",\"951247\",\"173384.45608\"],[\"1749735300000\",\"0.18183\",\"0.18286\",\"0.18176\",\"0.18253\",\"1351040\",\"246412.52721\"],[\"1749735000000\",\"0.18119\",\"0.18227\",\"0.18096\",\"0.18183\",\"1453484\",\"263973.52548\"],[\"1749734700000\",\"0.18121\",\"0.18131\",\"0.18054\",\"0.18119\",\"815236\",\"147542.43212\"],[\"1749734400000\",\"0.18202\",\"0.18211\",\"0.18094\",\"0.18121\",\"2587563\",\"469221.98251\"],[\"1749734100000\",\"0.18212\",\"0.18242\",\"0.18192\",\"0.18202\",\"653428\",\"119023.8949\"],[\"1749733800000\",\"0.18178\",\"0.18248\",\"0.18178\",\"0.18212\",\"643117\",\"117119.17096\"],[\"1749733500000\",\"0.1813\",\"0.18214\",\"0.18112\",\"0.18178\",\"614820\",\"111726.52854\"],[\"1749733200000\",\"0.18116\",\"0.18145\",\"0.18077\",\"0.1813\",\"467661\",\"84741.59062\"],[\"1749732900000\",\"0.18093\",\"0.18137\",\"0.18083\",\"0.18116\",\"1657667\",\"300300.33101\"],[\"1749732600000\",\"0.18166\",\"0.18168\",\"0.18075\",\"0.18093\",\"1462363\",\"264904.23641\"],[\"1749732300000\",\"0.18238\",\"0.18245\",\"0.18152\",\"0.18166\",\"1051486\",\"191287.7732\"],[\"1749732000000\",\"0.18236\",\"0.18276\",\"0.18215\",\"0.18238\",\"1307610\",\"238565.59052\"],[\"1749731700000\",\"0.18234\",\"0.18306\",\"0.18211\",\"0.18236\",\"1520218\",\"277401.74302\"],[\"1749731400000\",\"0.18134\",\"0.18265\",\"0.18107\",\"0.18234\",\"4148622\",\"755319.54856\"],[\"1749731100000\",\"0.18135\",\"0.18173\",\"0.18097\",\"0.18134\",\"1093770\",\"198349.81532\"],[\"1749730800000\",\"0.18059\",\"0.18188\",\"0.1804\",\"0.18135\",\"2121143\",\"384472.95533\"],[\"1749730500000\",\"0.17981\",\"0.18113\",\"0.17974\",\"0.18059\",\"1476018\",\"266337.13807\"],[\"1749730200000\",\"0.17858\",\"0.1799\",\"0.17845\",\"0.17981\",\"3489293\",\"625142.17136\"],[\"1749729900000\",\"0.18039\",\"0.18039\",\"0.17728\",\"0.17858\",\"7850572\",\"1401941.41075\"],[\"1749729600000\",\"0.17999\",\"0.18051\",\"0.1796\",\"0.18039\",\"1931467\",\"347594.6825\"],[\"1749729300000\",\"0.1805\",\"0.18054\",\"0.17978\",\"0.17999\",\"989620\",\"178272.28388\"],[\"1749729000000\",\"0.18072\",\"0.18109\",\"0.18021\",\"0.1805\",\"573134\",\"103485.53595\"],[\"1749728700000\",\"0.18104\",\"0.18145\",\"0.18061\",\"0.18072\",\"1254413\",\"227095.73352\"],[\"1749728400000\",\"0.18026\",\"0.18152\",\"0.18025\",\"0.18104\",\"1360807\",\"246501.83742\"],[\"1749728100000\",\"0.17982\",\"0.18078\",\"0.17964\",\"0.18026\",\"1012770\",\"182459.7459\"],[\"1749727800000\",\"0.18068\",\"0.18073\",\"0.17976\",\"0.17982\",\"2368334\",\"426887.74373\"],[\"1749727500000\",\"0.1809\",\"0.18095\",\"0.18035\",\"0.18068\",\"2012707\",\"363655.29499\"]]},\"retExtInfo\":{},\"time\":1749930262541}";
                BybitKlineResponse klineResponse = new ObjectMapper().readValue(responseBody, BybitKlineResponse.class);
                BybitKlineResult result = klineResponse.getResult();
                return result.getList();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    public int getLowTfSignal(double lastPrice, Pivot firstPivot, Pivot lastPivot, double atr, double volumeOsc) {
        if (volumeOsc < settings.volumeOsc) return 0;

        double fp = firstPivot.getPivot();
        double lp = lastPivot.getPivot();
        double between = Math.abs(fp - lp);
        double toFirst = Math.abs(lastPrice - fp);

        if (fp < lp) {                    // long
            if (between > 2 * atr) {
                if (lastPrice >= fp && lastPrice < lp) return 1;
            } else {
                if (toFirst <= 1.5 * atr && lastPrice < fp) return 1; // плотный каскад
            }
        } else if (fp > lp) {             // short
            if (between > 2 * atr) {
                if (lastPrice <= fp && lastPrice > lp) return -1;
            } else {
                if (toFirst <= 1.5 * atr && lastPrice > fp) return -1; // плотный каскад
            }
        }
        return 0;
    }


    public int getLowTfSignalForEntryBeforeFirstPivot(double lastPrice, Pivot pivot, double atr, double volumeOsc) {
        double pivotPrice = pivot.getPivot();
        double distanceToPivot = Math.abs(lastPrice - pivotPrice);

        if (distanceToPivot <= 2 * atr && volumeOsc >= settings.volumeOsc) {
            return lastPrice < pivotPrice ? 1 : -1;
        }

        return 0; // Нет сигнала
    }

}
