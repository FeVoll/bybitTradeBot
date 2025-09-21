package com.fevoll.binanceAutoTrader.service.tradingServices.watcher;

import com.fevoll.binanceAutoTrader.dto.BybitTickersResponseResult;
import com.fevoll.binanceAutoTrader.service.info.BybitTickersInfoService;
import com.fevoll.binanceAutoTrader.service.info.CorrelationService;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TickersService {

    @Autowired
    Settings settings;

    @Autowired
    BybitTickersInfoService tickersInfoService;

    @Autowired
    CorrelationService correlationService;

    @Getter
    private List<String> tickers = new ArrayList<>();

    public void addTicker(String symbol){
        tickers.add(symbol);
    }

    public void removeTicker(String symbol){
        tickers.remove(symbol);
    }

    public void clearTicker(){
        tickers.clear();
    }

    //@PostConstruct
    //@Scheduled(fixedRate = 1000)
    //@Scheduled(cron = "0 0 * * * *")
    //@RequestMapping(value = "/updateTickers")
    void updateTickers(){
        if (settings.volumeWatchList){
            List<BybitTickersResponseResult> tempTickers = tickersInfoService.actualTickersInfo();
            tickers = tempTickers.stream()
                    .map(BybitTickersResponseResult::getSymbol)
                    .toList();
        }
    }


    @PostConstruct
   //@Scheduled(fixedRate = 1000)
    @Scheduled(cron = "0 */10 * * * *")
    //@RequestMapping(value = "/updateTickers")
    void updateTickersWithBtcCorrCheck(){
        if (settings.volumeWatchList){
            List<BybitTickersResponseResult> allTickers = tickersInfoService.actualTickersInfo();
            
            // Создаем пул потоков для параллельной обработки
            int poolSize = Math.min(10, allTickers.size());
            Thread[] workers = new Thread[poolSize];
            List<BybitTickersResponseResult>[] resultsPerThread = new List[poolSize];
            
            // Распределяем работу между потоками
            for (int i = 0; i < poolSize; i++) {
                int finalI = i;
                workers[i] = new Thread(() -> {
                    List<BybitTickersResponseResult> threadResults = new ArrayList<>();
                    for (int j = finalI; j < allTickers.size(); j += poolSize) {
                        BybitTickersResponseResult ticker = allTickers.get(j);
                        try {
                            if (correlationService.prepareAndCalculateCorrelations(ticker.getSymbol())) {
                                threadResults.add(ticker);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    resultsPerThread[finalI] = threadResults;
                });
                workers[i].start();
            }

            // Ждем завершения всех потоков
            for (Thread worker : workers) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Собираем результаты
            List<BybitTickersResponseResult> filteredTickers = new ArrayList<>();
            for (List<BybitTickersResponseResult> part : resultsPerThread) {
                if (part != null) {
                    filteredTickers.addAll(part);
                }
            }

            tickers = filteredTickers.stream()
                    .map(BybitTickersResponseResult::getSymbol)
                    .toList();
        }
    }

}
