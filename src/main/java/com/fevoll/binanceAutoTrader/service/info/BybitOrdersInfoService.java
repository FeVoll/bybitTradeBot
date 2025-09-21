package com.fevoll.binanceAutoTrader.service.info;

import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fevoll.binanceAutoTrader.config.Encryption;
import com.fevoll.binanceAutoTrader.service.tradingServices.openPositionMonitoring.OpenPositionsMonitoringService;
import com.fevoll.binanceAutoTrader.service.tradingServices.transformers.OpenOrderTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class BybitOrdersInfoService {

    @Autowired
    OpenOrderTransformer orderTransformer;

    @Autowired
    OpenPositionsMonitoringService openPositionsMonitoring;

    @Autowired
    Encryption encryption;
    @Value("${bybit.url}")
    public String url;

    //Обновляем открытые позиции
    public void updateOpenOrders() {
        var client = BybitApiClientFactory.newInstance(encryption.API_KEY, encryption.API_SECRET, url).newAsyncPositionRestClient();
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).settleCoin("USDT").build();
        client.getPositionInfo(positionListRequest, response -> {
            try {
                orderTransformer.transformOpenOrders(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void runOrdersMonitoring(){
        var client = BybitApiClientFactory.newInstance(encryption.API_KEY, encryption.API_SECRET, url).newAsyncPositionRestClient();
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).settleCoin("USDT").build();
        client.getPositionInfo(positionListRequest, response -> {
            try {
                openPositionsMonitoring.check(response.toString());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
