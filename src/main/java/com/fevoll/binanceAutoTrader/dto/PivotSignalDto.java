package com.fevoll.binanceAutoTrader.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PivotSignalDto {

    int signal;
    Pivot pivot;
    Pivot firstPivot;
}
