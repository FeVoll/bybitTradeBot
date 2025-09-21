package com.fevoll.binanceAutoTrader.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LowTfSignalDto {
    public final int    side;       // 1 long, -1 short, 0 none
    public final double entry;      // цена входа
    public final Double stopLoss;   // SL, null если side==0
    public final Double takeProfit; // TP, null если side==0
}
