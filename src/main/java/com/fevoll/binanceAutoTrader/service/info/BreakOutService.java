package com.fevoll.binanceAutoTrader.service.info;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fevoll.binanceAutoTrader.dto.BybitKlineResponse;
import com.fevoll.binanceAutoTrader.service.tradingServices.api.BybitApiService;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import com.fevoll.binanceAutoTrader.service.tradingServices.transformers.BreakoutFinderV2;
import com.fevoll.binanceAutoTrader.telegram.Bot;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;


@Service
public class BreakOutService {

    private final ObjectMapper objectMapper;

    @Autowired
    BreakoutFinderV2 breakoutFinder;

    @Autowired
    BybitApiService apiService;
    @Autowired
    Settings settings;
    @Autowired
    Bot bot;

    private LocalDateTime fetchError = LocalDateTime.MIN;

    public BreakOutService() {
        this.objectMapper = new ObjectMapper();
    }

    public void startBreakoutCheck(String symbol) {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(1000));

        long start = from.toEpochMilli();
        long end = now.toEpochMilli();

        for (String timeframe : settings.activeTimeframes){
            try (Response response = apiService.getCline(symbol, start, end,1000, timeframe).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    // Маппинг JSON-ответа в объект Java
                    //responseBody = readFromFile("src/main/resources/cline.txt");
                    BybitKlineResponse klineResponse = objectMapper.readValue(responseBody, BybitKlineResponse.class);

                    breakoutFinder.run(klineResponse, timeframe);

                } else {
                    System.err.println("Failed to fetch data: " + response.message());
                    if (fetchError.isBefore(LocalDateTime.now())){
                        bot.sendErrorNotification();
                        fetchError = LocalDateTime.now().plusMinutes(10);
                    }


                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String readFromFile(String filePath) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
    }
}
