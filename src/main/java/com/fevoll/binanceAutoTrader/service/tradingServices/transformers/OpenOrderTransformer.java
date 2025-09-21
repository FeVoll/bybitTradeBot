package com.fevoll.binanceAutoTrader.service.tradingServices.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OpenOrderTransformer {

    private final HashMap<String, String> symbols = new HashMap<>();

    private final HashMap<String, LocalDateTime> ordersHistory = new HashMap<>();

    public void transformOpenOrders(Object openOrders) throws JsonProcessingException {
        Set<String> tempSymbols = new HashSet<>();
        System.out.println(symbols + "symbolsSet");
        Pattern retMsgPattern = Pattern.compile("retMsg=OK");
        Matcher retMsgMatcher = retMsgPattern.matcher(openOrders.toString());

        if (retMsgMatcher.find()) {
            Pattern pattern = Pattern.compile("symbol=([A-Za-z0-9-]+)");
            Matcher matcher = pattern.matcher(openOrders.toString());

            while (matcher.find()) {
                tempSymbols.add(matcher.group(1));
            }
            System.out.println(tempSymbols + "tempSymbols");
            // удаляет элемент из map через итератор
            symbols.keySet().removeIf(key -> !tempSymbols.contains(key));

            System.out.println(symbols + "finished");
        } else {
            // Если retMsg не равен OK, выводим сообщение или обрабатываем ошибку
            System.out.println("retMsg is not OK. Aborting symbol extraction.");
        }

    }

    // Добавление символа в список открытых ордеров
    public void addSymbol(String symbol, String side) {
        symbols.put(symbol, side);  // synchronizedSet обеспечивает потокобезопасность
    }

    public void addTrade(String symbol) {
        ordersHistory.put(symbol, LocalDateTime.now());  // synchronizedSet обеспечивает потокобезопасность
    }

    // Удаление символа из списка открытых ордеров
    public void removeSymbol(String symbol) {
        symbols.remove(symbol);  // synchronizedSet обеспечивает потокобезопасность
    }

    // Проверка наличия символа в списке открытых ордеров
    public boolean containsSymbol(String symbol) {
        return symbols.containsKey(symbol);  // synchronizedSet обеспечивает потокобезопасность
    }

    // Получение копии текущего списка символов
    public HashMap<String, String> getSymbols() {
        synchronized (symbols) {
            return new HashMap<>(symbols); // Возвращаем новую копию списка для потокобезопасного доступа
        }
    }

    public HashMap<String, LocalDateTime> getOrdersHistory() {
        synchronized (ordersHistory) {
            return new HashMap<>(ordersHistory); // Возвращаем новую копию списка для потокобезопасного доступа
        }
    }

    public boolean hasRecentTrade(String symbol, long hours) {
        return Optional.ofNullable(ordersHistory.get(symbol))
                .map(t -> t.isAfter(LocalDateTime.now().minusHours(hours)))
                .orElse(false);
    }


    public long getBuyCounts(){
        return symbols.values().stream().filter("Buy"::equals).count();
    }
    public long getSellCounts(){
        return symbols.values().stream().filter("Sell"::equals).count();
    }

}
