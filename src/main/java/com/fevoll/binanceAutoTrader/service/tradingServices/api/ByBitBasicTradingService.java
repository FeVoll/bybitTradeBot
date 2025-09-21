package com.fevoll.binanceAutoTrader.service.tradingServices.api;

import com.bybit.api.client.restApi.BybitApiAsyncTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fevoll.binanceAutoTrader.config.Encryption;

import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ByBitBasicTradingService {


    @Autowired
    Settings settings;
    @Autowired
    Encryption encryption;

    @Value("${bybit.url}")
    public String url;

    public void createOrder(String symbol, String side, Double lastPrice, String tp, String sl){

        String qty = String.valueOf(Math.round((settings.orderAmount * settings.leverage) / lastPrice));

        BybitApiClientFactory factory = BybitApiClientFactory.newInstance(encryption.API_KEY, encryption.API_SECRET, url);
        BybitApiAsyncTradeRestClient client = factory.newAsyncTradeRestClient();
        Map<String, Object> order =Map.of(
                "category", "linear",
                "symbol", symbol,
                "side", side,
                "positionIdx",0,
                "orderType", "Market",
                "qty", qty,
                "timeInForce", "GTC",
                "takeProfit", tp,
                "stopLoss", sl
        );
        ObjectMapper mapper = new ObjectMapper();

        client.createOrder(order, response -> {
            try {
                String json = mapper.writeValueAsString(response);
                checkResponse(json, symbol, side);
            } catch (JsonProcessingException e) {
                System.err.println("❌ Failed to serialize order response to JSON: " + e.getMessage());
            }
        });
    }

    public void checkResponse(String response, String symbol, String side) {
        /*try {
            JSONObject jsonResponse = new JSONObject(response);

            int retCode = jsonResponse.optInt("retCode", -1);
            String retMsg = jsonResponse.optString("retMsg", "");

            if (retCode == 0 && "OK".equalsIgnoreCase(retMsg)) {
                JSONObject result = jsonResponse.optJSONObject("result");
                String orderId = result != null ? result.optString("orderId", "N/A") : "N/A";


                System.out.printf("✅ Order placed | Symbol: %s | Side: %s | OrderID: %s\n", symbol, side, orderId);
            } else {
                System.err.printf("❌ Order failed | Symbol: %s | Side: %s | Reason: %s\n", symbol, side, retMsg);
            }

        } catch (JSONException e) {
            System.err.println("❌ Failed to parse order response JSON: " + e.getMessage());
        }

         */
    }

}
