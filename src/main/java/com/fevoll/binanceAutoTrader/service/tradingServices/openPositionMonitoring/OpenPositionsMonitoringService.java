package com.fevoll.binanceAutoTrader.service.tradingServices.openPositionMonitoring;


import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fevoll.binanceAutoTrader.config.Encryption;
import com.fevoll.binanceAutoTrader.dto.BeMonitorDto;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import com.fevoll.binanceAutoTrader.telegram.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class OpenPositionsMonitoringService {

    @Autowired
    Bot bot;

    @Autowired
    Encryption encryption;
    @Value("${bybit.url}")
    public String url;

    @Autowired
    Settings settings;

    HashMap<String, BeMonitorDto> be = new HashMap<>();

    public void check(String openOrders) throws TelegramApiException {
        Pattern retMsgPattern = Pattern.compile("retMsg=OK");
        Matcher retMsgMatcher = retMsgPattern.matcher(openOrders);

        if (retMsgMatcher.find()) {
            // Extract symbol
            Pattern symbolPattern = Pattern.compile("symbol=([A-Za-z0-9-]+)");
            Matcher symbolMatcher = symbolPattern.matcher(openOrders);

            // Extract avgPrice
            Pattern avgPricePattern = Pattern.compile("avgPrice=([0-9.]+)");
            Matcher avgPriceMatcher = avgPricePattern.matcher(openOrders);

            // Extract markPrice
            Pattern markPricePattern = Pattern.compile("markPrice=([0-9.]+)");
            Matcher markPriceMatcher = markPricePattern.matcher(openOrders);

            // Extract side
            Pattern sidePattern = Pattern.compile("side=([A-Za-z]+)");
            Matcher sideMatcher = sidePattern.matcher(openOrders);

            // Extract stopLoss (optional)
            Pattern stopLossPattern = Pattern.compile("stopLoss=([0-9.]*)");
            Matcher stopLossMatcher = stopLossPattern.matcher(openOrders);

            // Extract takeProfit (optional)
            Pattern takeProfitPattern = Pattern.compile("takeProfit=([0-9.]*)");
            Matcher takeProfitMatcher = takeProfitPattern.matcher(openOrders);

            while (symbolMatcher.find() && avgPriceMatcher.find() && markPriceMatcher.find()
                    && sideMatcher.find() && stopLossMatcher.find() && takeProfitMatcher.find()) {

                String symbol = symbolMatcher.group(1);
                double avgPrice = Double.parseDouble(avgPriceMatcher.group(1));
                double markPrice = Double.parseDouble(markPriceMatcher.group(1));



                // double changePercentage = ((markPrice - avgPrice) / avgPrice) * 100;

                String side = sideMatcher.group(1);

                String stopLossStr = stopLossMatcher.group(1);
                double stopLoss = stopLossStr.isEmpty() ? Double.NaN : Double.parseDouble(stopLossStr);

                /*
                String takeProfitStr = takeProfitMatcher.group(1);
                double takeProfit = takeProfitStr.isEmpty() ? Double.NaN : Double.parseDouble(takeProfitStr);

                System.out.printf("Symbol: %s, Entry Price: %.4f, Current Price: %.4f, Change: %.2f%%, Side: %s, SL: %s, TP: %s%n",
                        symbol, avgPrice, markPrice, changePercentage, side,
                        Double.isNaN(stopLoss) ? "N/A" : String.format("%.4f", stopLoss),
                        Double.isNaN(takeProfit) ? "N/A" : String.format("%.4f", takeProfit));

                 */
                if (be.containsKey(symbol)) {
                    BeMonitorDto mon = be.get(symbol);
                    double bePrice   = mon.getBe();
                    double topPivot  = mon.getTopPivot();        // новый уровень

                    if ("Buy".equals(side)) {

                        // 1) перевод в б/у
                        if (markPrice >= bePrice && (Double.isNaN(stopLoss) || stopLoss < avgPrice)) {
                            updateSl(symbol, String.valueOf(avgPrice * 1.002),  true);
                            // be.remove(symbol);           // НЕ удаляем — понадобится для трейлинга
                        }

                        // 2) половинный трейлинг
                        if (markPrice > topPivot && settings.enableSafeTrailingSL) {
                            double pnlPct     = (markPrice - avgPrice) / avgPrice;      // 0.013 → 1.3 %
                            double newSlPrice = avgPrice * (1 + pnlPct * 0.5);          // ½ PnL
                            if (Double.isNaN(stopLoss) || newSlPrice > stopLoss) {      // только повышаем SL
                                updateSl(symbol, String.valueOf(newSlPrice), false);
                            }
                        }

                    } else if ("Sell".equals(side)) {

                        if (markPrice <= bePrice && (Double.isNaN(stopLoss) || stopLoss > avgPrice)) {
                            updateSl(symbol, String.valueOf(avgPrice * 0.998), true);
                            // be.remove(symbol);
                        }

                        if (markPrice < topPivot && settings.enableSafeTrailingSL) {
                            double pnlPct     = (avgPrice - markPrice) / avgPrice;
                            double newSlPrice = avgPrice * (1 - pnlPct * 0.5);
                            if (Double.isNaN(stopLoss) || newSlPrice < stopLoss) {      // только опускаем SL ближе
                                updateSl(symbol, String.valueOf(newSlPrice), false);
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("Response not OK. " + openOrders);
        }

    }

    public void updateSl(String symbol, String price, boolean notify) throws TelegramApiException {
        var client = BybitApiClientFactory.newInstance(encryption.API_KEY, encryption.API_SECRET, url).newTradeRestClient();
        var openLinearOrdersResult = client.getOpenOrders(TradeOrderRequest.builder().category(CategoryType.LINEAR).symbol(symbol)
                .openOnly(0).build());

        Pattern orderPattern = Pattern.compile("orderId=([a-zA-Z0-9-]+)(?=[^{}]*stopOrderType=StopLoss)");
        Matcher matcher = orderPattern.matcher(openLinearOrdersResult.toString());

        if (matcher.find()) {
            String orderId = matcher.group(1);

            var amendOrderRequest = TradeOrderRequest.builder().category(CategoryType.LINEAR).symbol(symbol)
                    .orderId(orderId)
                    .triggerPrice(price)
                    .build();
            System.out.println("Changed sl for "+ symbol + " " + client.amendOrder(amendOrderRequest));
            if (notify) bot.sendSlChangeNotification(symbol);

        } else {
            System.out.println("No StopLoss order found.");
        }
    }

    public void addBe(String symbol, Double change, Double trailingPivot){
        be.put(symbol, new BeMonitorDto(change, trailingPivot));
    }

}
