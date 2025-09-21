package com.fevoll.binanceAutoTrader.repository;

import com.fevoll.binanceAutoTrader.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
}

