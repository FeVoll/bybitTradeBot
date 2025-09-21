package com.fevoll.binanceAutoTrader.service.tradingServices.watcher;

import com.fevoll.binanceAutoTrader.service.info.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RestController
public class AdvancedWatcherService {


    @Autowired
    BybitOrdersInfoService ordersInfoService;

    @Autowired
    WorkingStatus status;

    @Autowired
    TickersService tickersService;

    @Autowired
    BreakOutService breakOutService;

    @Scheduled(fixedDelay = 300)
    void run() throws IOException {
        List<String> all = new ArrayList<>(tickersService.getTickers());
        if (!status.isWorking() || all.isEmpty()) return;

        // если больше 50 — тасуем и берём первые 50
        Collections.shuffle(all);
        List<String> batch = all.subList(0, Math.min(25, all.size()));

        System.out.println("Start " + LocalDateTime.now());
        int poolSize = Math.min(100, batch.size());
        ExecutorService exec = Executors.newFixedThreadPool(poolSize);
        try {
            CompletableFuture<?>[] futures = batch.stream()
                    .map(t -> CompletableFuture.runAsync(() -> {
                        try {
                            breakOutService.startBreakoutCheck(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, exec))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        } finally {
            exec.shutdown();
        }
    }


    @Scheduled(cron = "0 0 * * * *")
    @PostConstruct
    void updateSymbols(){
        System.out.println("Run check");
        ordersInfoService.updateOpenOrders();
    }



}


