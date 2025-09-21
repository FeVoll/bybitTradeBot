package com.fevoll.binanceAutoTrader.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "trade_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDetails {

    @Id
    private String orderId;
    private String timeframe;
    private Double entryPrice;
    private Double strategy;
    private Double numberOfPivots;
    private Double pivotIndexes;
    private Double volumeOsc5m;
    private Double volumeOscMainTf;

}
