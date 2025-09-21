package com.fevoll.binanceAutoTrader.service.info;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class WorkingStatus {

    @Getter
    private boolean isWorking = true;

    public void changeStatus(){
        isWorking = !isWorking;
    }

}
