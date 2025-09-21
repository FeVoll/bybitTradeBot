package com.fevoll.binanceAutoTrader.service.info;

import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.restApi.BybitApiAsyncPositionRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fevoll.binanceAutoTrader.config.Encryption;
import com.fevoll.binanceAutoTrader.entity.Trade;
import com.fevoll.binanceAutoTrader.repository.TradeRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class JournalService {

    @Value("${bybit.url}")
    public String url;
    @Autowired
    Encryption encryption;
    @Autowired
    TradeRepository tradeRepository;

    @Scheduled(fixedRate = 3600000) // раз в час
    public void updateJournal() {

        BybitApiClientFactory factory = BybitApiClientFactory.newInstance(encryption.API_KEY, encryption.API_SECRET, url);
        BybitApiAsyncPositionRestClient client = factory.newAsyncPositionRestClient();

        var closPnlRequest = PositionDataRequest.builder()
                .category(CategoryType.LINEAR)
                .build();

        client.getClosePnlList(closPnlRequest, response -> {
            try {
                String json = new ObjectMapper().writeValueAsString(response);
                checkClosedPnl(json);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });


    }

    public void checkClosedPnl(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.optInt("retCode", -1) == 0) {
                JSONObject result = jsonResponse.optJSONObject("result");
                if (result != null) {
                    JSONArray list = result.optJSONArray("list");
                    if (list != null) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject tradeJson = list.getJSONObject(i);

                            String orderId = tradeJson.optString("orderId", null);
                            if (orderId == null || tradeRepository.existsById(orderId)) {
                                continue; // Уже есть или нет ID — пропускаем
                            }

                            String symbol = tradeJson.optString("symbol", "N/A");
                            String side = tradeJson.optString("side", "N/A");
                            // Меняем направление сделки на противоположное
                            if ("Buy".equalsIgnoreCase(side)) {
                                side = "Sell";
                            } else if ("Sell".equalsIgnoreCase(side)) {
                                side = "Buy";
                            }
                            String pnl = tradeJson.optString("closedPnl", "0");
                            String createdTime = tradeJson.optString("createdTime", "0");

                            LocalDateTime createdAt = Instant.ofEpochMilli(Long.parseLong(createdTime))
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();

                            Trade trade = Trade.builder()
                                    .orderId(orderId)
                                    .symbol(symbol)
                                    .side(side)
                                    .pnl(Double.parseDouble(pnl))
                                    .createdAt(createdAt)
                                    .build();

                            tradeRepository.save(trade);

                            System.out.printf("✅ New trade saved | Symbol: %s | OrderID: %s | Side: %s | Time: %s | PnL: %s\n",
                                    symbol, orderId, side, createdAt, pnl);
                        }
                    }
                }
            } else {
                System.err.println("API returned error: " + jsonResponse.optString("retMsg", "Unknown error"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
