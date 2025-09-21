package com.fevoll.binanceAutoTrader.service.tradingServices.openPositionMonitoring;

import com.fevoll.binanceAutoTrader.service.info.BybitOrdersInfoService;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenPositionsMonitoringTask {

    @Autowired
    Settings settings;

    @Autowired
    BybitOrdersInfoService ordersInfoService;

    @Scheduled(cron = "* * * * * *")
    public void runCheck(){
        if (settings.enableSafeMode){
            ordersInfoService.runOrdersMonitoring();
        }
    }
}
