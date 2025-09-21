package com.fevoll.binanceAutoTrader.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    private String orderId;
    private String symbol;
    private String side;
    private double pnl;
    private String timeframe;
    private String comment;
    private LocalDateTime createdAt;
}
