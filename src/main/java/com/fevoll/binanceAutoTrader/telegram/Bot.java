package com.fevoll.binanceAutoTrader.telegram;

import com.fevoll.binanceAutoTrader.service.info.WorkingStatus;
import com.fevoll.binanceAutoTrader.service.tradingServices.Settings;
import com.fevoll.binanceAutoTrader.service.tradingServices.watcher.TickersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Bot extends TelegramLongPollingBot {
    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String token;
    @Value("${bot.chatId}")
    private String chatId;

    private final LocalDateTime lastStartDate = LocalDateTime.now();

    @Autowired
    WorkingStatus status;

    @Autowired
    Settings settings;

    @Autowired
    TickersService tickersService;

    private boolean waitingForPositionInput = false;
    private boolean waitingForLeverageInput = false;
    private boolean waitingForVolumeInput = false;
    private boolean waitingForSensitivityInput = false;
    private boolean waitingForTpAtrInput = false;
    private boolean waitingForSlAtrInput = false;
    private boolean waitingForSuperTrendSensInput = false;
    private boolean waitingForEmaInput = false;
    private boolean waitingForTpPercentInput = false;
    private boolean waitingForSlPercentInput = false;
    private boolean waitingForCorrInput = false;
    private boolean waitingForAddSymbol = false;
    private boolean waitingForRemoveSymbol = false;
    private boolean waitingForActiveTradesInput = false;
    private boolean waitingForPivotPeriodInput = false;
    private boolean waitingForThresholdRateInput = false;
    private boolean waitingForMinTestsInput = false;
    private boolean waitingForAtrMultiplier = false;
    private boolean waitingForOscVolume = false;
    private boolean waitingForSafeModePivotInput = false;

    @Override
    public String getBotUsername() { return botName; }
    @Override
    public String getBotToken() { return token; }
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            if (messageText != null && !messageText.isEmpty()) {
                handleIncomingMessage(update.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery().getData(), update.getCallbackQuery().getMessage());
        }
    }

    private void handleIncomingMessage(Message message) {
        String userMessage = message.getText();
        long chatId = message.getChatId();

        try {
            if (waitingForPositionInput) {
                settings.orderAmount = Double.parseDouble(userMessage);
                sendMessage(chatId, "Position value set to: " + settings.orderAmount);
                waitingForPositionInput = false;
                sendMenu(chatId);
            } else if (waitingForLeverageInput) {
                settings.leverage = Double.parseDouble(userMessage);
                sendMessage(chatId, "Leverage set to: " + settings.leverage);
                waitingForLeverageInput = false;
                sendMenu(chatId);
            } else if (waitingForVolumeInput) {
                settings.watchlistVolume = Integer.parseInt(userMessage);
                sendMessage(chatId, "Watchlist Volume set to: " + settings.watchlistVolume);
                waitingForVolumeInput = false;
                sendMenu(chatId);
            } else if (waitingForSensitivityInput) {
                settings.sidewaySens = Double.parseDouble(userMessage);
                sendMessage(chatId, "Sideways Sensitivity set to: " + settings.sidewaySens);
                waitingForSensitivityInput = false;
                sendMenu(chatId);
            } else if (waitingForSuperTrendSensInput) {
                settings.superTrendSens = Double.parseDouble(userMessage);
                sendMessage(chatId, "SuperTrend Sensitivity set to: " + settings.superTrendSens);
                waitingForSuperTrendSensInput = false;
                sendMenu(chatId);
            } else if (waitingForEmaInput) {
                settings.ema = Integer.parseInt(userMessage);
                sendMessage(chatId, "Ema Length set to: " + settings.ema);
                waitingForEmaInput = false;
                sendMenu(chatId);
            } else if (waitingForTpAtrInput) {
                settings.tpAtr = Double.parseDouble(userMessage);
                sendMessage(chatId, "Take Profit ATR Multiplier set to: " + settings.tpAtr);
                waitingForTpAtrInput = false;
                sendMenu(chatId);
            } else if (waitingForSlAtrInput) {
                settings.slAtr = Double.parseDouble(userMessage);
                sendMessage(chatId, "Stop Loss ATR Multiplier set to: " + settings.slAtr);
                waitingForSlAtrInput = false;
                sendMenu(chatId);
            } else if (waitingForTpPercentInput) {
                settings.tpPercent = Double.parseDouble(userMessage);
                sendMessage(chatId, "Take Profit percentage set to: " + settings.tpPercent + "%");
                waitingForTpPercentInput = false;
                sendMenu(chatId);
            } else if (waitingForSlPercentInput) {
                settings.slPercent = Double.parseDouble(userMessage);
                sendMessage(chatId, "Stop Loss percentage set to: " + settings.slPercent + "%");
                waitingForSlPercentInput = false;
                sendMenu(chatId);
            } else if (waitingForCorrInput) {
                settings.btcCorrEntries = Integer.parseInt(userMessage);
                settings.checkBTCCorr = true;
                sendMessage(chatId, "Check BTC Correlation is now enabled");
                waitingForCorrInput = false;
                sendMenu(chatId);
            } else if (waitingForRemoveSymbol) {
                tickersService.removeTicker(userMessage);
                sendMessage(chatId, "Symbol removed");
                waitingForRemoveSymbol = false;
                sendMenu(chatId);
            } else if (waitingForAddSymbol) {
                tickersService.addTicker(userMessage);
                sendMessage(chatId, "Symbol added");
                waitingForAddSymbol = false;
                sendMenu(chatId);
            } else if (waitingForActiveTradesInput) {
                settings.activeTrades = Integer.parseInt(userMessage);
                sendMessage(chatId, "Active Trades set to: " + settings.activeTrades);
                waitingForActiveTradesInput = false;
                sendMenu(chatId);
            } else if (waitingForPivotPeriodInput) {
                settings.prd = Integer.parseInt(userMessage);
                sendMessage(chatId, "Pivot Period set to: " + settings.prd);
                waitingForPivotPeriodInput = false;
                sendBreakoutConfigMenu(chatId);
            } else if (waitingForThresholdRateInput) {
                settings.thresholdRate = Double.parseDouble(userMessage);
                sendMessage(chatId, "Threshold Rate set to: " + settings.thresholdRate);
                waitingForThresholdRateInput = false;
                sendBreakoutConfigMenu(chatId);
            } else if (waitingForMinTestsInput) {
                settings.minTests = Integer.parseInt(userMessage);
                sendMessage(chatId, "Minimum Number of Tests set to: " + settings.minTests);
                waitingForMinTestsInput = false;
                sendBreakoutConfigMenu(chatId);
            } else if (waitingForAtrMultiplier) {
                settings.atrMultiplier = Integer.parseInt(userMessage);
                sendMessage(chatId, "Atr Multiplier set to: " + settings.atrMultiplier);
                waitingForAtrMultiplier = false;
                sendBreakoutConfigMenu(chatId);
            } else if (waitingForOscVolume) {
                settings.volumeOsc = Integer.parseInt(userMessage);
                sendMessage(chatId, "Volume Osc set to: " + settings.volumeOsc);
                waitingForOscVolume = false;
                sendBreakoutConfigMenu(chatId);
            } else if (waitingForSafeModePivotInput) {
                settings.safeModePivotPercent = Double.parseDouble(userMessage);
                sendMessage(chatId, "Safe Mode Pivot Percent set to: " + settings.safeModePivotPercent + "%");
                waitingForSafeModePivotInput = false;
                sendMenu(chatId);
            }
            else {
                sendMenu(chatId);
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Invalid number. Please enter a valid number.");
        }
    }

    private void handleCallbackQuery(String callbackData, Message message) {
        long chatId = message.getChatId();

        switch (callbackData) {
            case "set_position_value":
                waitingForPositionInput = true;
                sendMessage(chatId, "Please enter the position value:");
                break;
            case "set_leverage":
                waitingForLeverageInput = true;
                sendMessage(chatId, "Please enter the leverage:");
                break;
            case "input_volume_filter":
                waitingForVolumeInput = true;
                sendMessage(chatId, "Please enter the Watchlist Volume:");
                break;
            case "add_symbol":
                waitingForAddSymbol = true;
                sendMessage(chatId, "Please enter symbol to add (in format like: BTCUSDT)");
                break;
            case "remove_symbol":
                waitingForRemoveSymbol = true;
                sendMessage(chatId, "Please enter symbol to remove (in format like: BTCUSDT)");
                break;
            case "clear_list":
                tickersService.clearTicker();
                sendMessage(chatId, "List cleared:");
                sendMenu(chatId);
                break;
            case "set_sideways_sensitivity":
                waitingForSensitivityInput = true;
                sendMessage(chatId, "Please enter the Sideways Sensitivity:");
                break;
            case "set_supertrend_sens":
                waitingForSuperTrendSensInput = true;
                sendMessage(chatId, "Please enter the SuperTrend Sensitivity:");
                break;
            case "set_ema":
                waitingForEmaInput = true;
                sendMessage(chatId, "Please enter new Ema length:");
                break;
            case "set_tp_atr":
                waitingForTpAtrInput = true;
                sendMessage(chatId, "Please enter the Take Profit ATR Multiplier:");
                break;
            case "set_sl_atr":
                waitingForSlAtrInput = true;
                sendMessage(chatId, "Please enter the Stop Loss ATR Multiplier:");
                break;
            case "set_tp_percent":
                waitingForTpPercentInput = true;
                sendMessage(chatId, "Please enter the Take Profit percentage (e.g., 1.2 for 1.2%):");
                break;
            case "set_sl_percent":
                waitingForSlPercentInput = true;
                sendMessage(chatId, "Please enter the Stop Loss percentage (e.g., 1.0 for 1%):");
                break;
            case "toggle_tp_method":
                settings.tpType = (settings.tpType < 3) ? settings.tpType + 1 : 1;
                sendMessage(chatId, "Take Profit method changed");
                sendMenu(chatId);
                break;
            case "toggle_sl_method":
                settings.slType = (settings.slType < 3) ? settings.slType + 1 : 1;
                sendMessage(chatId, "Stop Loss method changed");
                sendMenu(chatId);
                break;
            case "open_tp_sl_menu":
                sendTpSlMenu(chatId);  // Открываем подменю SL и TP
                break;
            case "configure_breakout":
                sendBreakoutConfigMenu(chatId);  // Открываем подменю прорывов
                break;
            case "toggle_enabled":
                status.changeStatus();
                sendMessage(chatId, "Bot is now " + (status.isWorking() ? "Enabled" : "Disabled"));
                sendMenu(chatId);
                break;
            case "set_safe_mode_pivot_percent":
                waitingForSafeModePivotInput = true;
                sendMessage(chatId, "Please enter the Safe Mode Pivot percentage (e.g., 60 for 60%):");
                break;
            case "change_watchlist_type":
                settings.volumeWatchList = !settings.volumeWatchList;
                sendMessage(chatId, "Watchlist type changed");
                sendMenu(chatId);
                break;
            case "toggle_corr":
                if (settings.checkBTCCorr) {
                    settings.checkBTCCorr = false;
                    settings.btcCorrEntries = 5;
                    sendMessage(chatId, "Check BTC Correlation is now disabled");
                    sendMenu(chatId);
                } else {
                    waitingForCorrInput = true;
                    sendMessage(chatId, "Enter the number of correlation trades allowed in a row (0 to avoid entering at all)");
                }
                break;
            case "input_symbols_filter":
                sendSymbolWatchListMenu(chatId);
                break;
            case "change_timeframe":
                sendTimeframeMenu(chatId);
                break;
            case "set_active_trades":
                waitingForActiveTradesInput = true;
                sendMessage(chatId, "Please enter the number of active trades allowed:");
                break;
            case "toggle_safe_mode":
                settings.enableSafeMode = !settings.enableSafeMode;
                sendMessage(chatId, "Safe Mode is now " + (settings.enableSafeMode ? "Enabled" : "Disabled"));
                sendMenu(chatId);
                break;
            case "set_pivot_period":
                waitingForPivotPeriodInput = true;
                sendMessage(chatId, "Please enter the Pivot Period (e.g., 10):");
                break;
            case "set_threshold_rate":
                waitingForThresholdRateInput = true;
                sendMessage(chatId, "Please enter the Threshold Rate as a decimal (e.g., 0.05 for 5%):");
                break;
            case "set_min_tests":
                waitingForMinTestsInput = true;
                sendMessage(chatId, "Please enter the Minimum Number of Tests (e.g., 3):");
                break;
            case "set_atr_multiplier":
                waitingForAtrMultiplier = true;
                sendMessage(chatId, "Please enter the Atr Multiplier (e.g., 5):");
                break;
            case "set_volume_osc":
                waitingForOscVolume = true;
                sendMessage(chatId, "Please enter the Osc Volume (e.g., 30):");
                break;
            case "back_to_main_menu":
                sendMenu(chatId);
                break;
            default:
                if (callbackData.startsWith("timeframe_")) {
                    String tf = callbackData.replace("timeframe_", "");

                    if (settings.activeTimeframes.contains(tf)) {
                        settings.activeTimeframes.remove(tf);
                        sendMessage(chatId, "Removed timeframe: " + formatTimeframeDisplay(tf));
                    } else {
                        settings.activeTimeframes.add(tf);
                        sendMessage(chatId, "Added timeframe: " + formatTimeframeDisplay(tf));
                    }

                    sendTimeframeMenu(chatId);
                } else {
                    sendMessage(chatId, "Invalid option. Please try again.");
                    sendMenu(chatId);
                }
                break;
        }
    }

    public void sendMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        String infoMessage = "🤖 <b>Bot Status V2:</b> " + (status.isWorking() ? "🟢 <b>Enabled</b>" : "🔴 <b>Disabled</b> ") + lastStartDate + " \n\n" +
                "💰 <b>Position Value:</b> " + settings.orderAmount + " 💵 per order (without leverage)\n" +
                "⚖️ <b>Leverage:</b> " + settings.leverage + "x\n" +
                "⏱️ <b>Timeframes:</b> " + String.join(", ", settings.activeTimeframes.stream()
                        .map(tf -> tf.equals("60") ? "1h" : tf.equals("120") ? "2h" : tf.equals("180") ? "3h" :
                                tf.equals("240") ? "4h" : tf.equals("D") ? "1d" : tf.equals("W") ? "1w" : tf + "m")
                        .toArray(String[]::new)) + "\n" +
                "📊 <b>Watchlist:</b> " + (settings.volumeWatchList ? settings.watchlistVolume + " (Volume)" : "Custom list") + "\n" +
                "📈 <b>Ema Length:</b> " + settings.ema + "\n" +
                "🛡️ <b>Safe Mode:</b> " + (settings.enableSafeMode ? "Enabled (" + settings.safeModePivotPercent + " %)" : "Disabled") + "\n" +
                "🎯 <b>Take Profit:</b> " +
                (settings.tpType == 1 ? settings.tpPercent + "% (Fixed)" :
                        settings.tpType == 2 ? settings.tpAtr + " ATR" :
                                "Smart Levels") + "\n" +
                "🚨 <b>Stop Loss:</b> " +
                (settings.slType == 1 ? settings.slPercent + "% (Fixed)" :
                        settings.slType == 2 ? settings.slAtr + " ATR" :
                                "Smart Levels") + "\n" +
                "📊 <b>Active Trades Allowed:</b> " + settings.activeTrades + "\n\n" +
                "📐 <b>Breakout Settings:</b>\n" +
                "🔹 <b>Pivot Period:</b> <i>" + settings.prd + "</i>\n" +
                "🔹 <b>Threshold Rate:</b> <i>" + settings.thresholdRate + "</i>\n" +
                "🔹 <b>Minimum Tests:</b> <i>" + settings.minTests + "</i>\n\n" +
                "🔹 <b>Volume Osc:</b> <i>" + settings.volumeOsc + "</i>\n\n" +
                "👉 <b>Choose an option:</b>";

        message.enableHtml(true);
        message.setText(infoMessage);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Toggle bot status
        List<InlineKeyboardButton> enableRow = new ArrayList<>();
        InlineKeyboardButton toggleButton = new InlineKeyboardButton();
        toggleButton.setText(status.isWorking() ? "🛑 Disable Bot" : "✅ Enable Bot");
        toggleButton.setCallbackData("toggle_enabled");
        enableRow.add(toggleButton);

        // Set position and leverage (row with two buttons)
        List<InlineKeyboardButton> moneyRow = new ArrayList<>();
        moneyRow.add(createInlineButton("💵 Set Position Value", "set_position_value"));
        moneyRow.add(createInlineButton("⚖️ Set Leverage", "set_leverage"));

        // Breakout configuration (row with one button)
        List<InlineKeyboardButton> breakoutRow = new ArrayList<>();
        breakoutRow.add(createInlineButton("📐 Configure Breakout", "configure_breakout"));

        // EMA and Timeframe (row with two buttons)
        List<InlineKeyboardButton> emaRow = new ArrayList<>();
        emaRow.add(createInlineButton("📈 Change EMA Length", "set_ema"));
        emaRow.add(createInlineButton("⏳ Change Timeframe", "change_timeframe"));

        // Safe Mode toggle (row with one button)
        List<InlineKeyboardButton> rowSafeMode = new ArrayList<>();
        rowSafeMode.add(createInlineButton("🛡️ Toggle Safe Mode", "toggle_safe_mode"));
        rowSafeMode.add(createInlineButton("🛡️ Set Safe Mode Pivot %", "set_safe_mode_pivot_percent"));

        // Configure SL/TP (row with one button)
        List<InlineKeyboardButton> slTpRow = new ArrayList<>();
        slTpRow.add(createInlineButton("⚙️ Configure SL/TP", "open_tp_sl_menu"));

        // Watchlist configuration (row with two buttons)
        List<InlineKeyboardButton> watchListRow = new ArrayList<>();
        watchListRow.add(createInlineButton(settings.volumeWatchList ? "📊 Change Watchlist Volume" : "📊 Change Watchlist Symbols", settings.volumeWatchList ? "input_volume_filter" : "input_symbols_filter"));
        watchListRow.add(createInlineButton("🔄 Change Watchlist Type", "change_watchlist_type"));

        // Active trades configuration (row with one button)
        List<InlineKeyboardButton> rowActiveTrades = new ArrayList<>();
        rowActiveTrades.add(createInlineButton("🔄 Set Active Trades", "set_active_trades"));

        // Add all rows to the menu
        rows.add(enableRow);
        rows.add(moneyRow);
        rows.add(breakoutRow);
        rows.add(emaRow);
        rows.add(rowSafeMode);
        rows.add(slTpRow);
        rows.add(watchListRow);
        rows.add(rowActiveTrades);
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTimeframeMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        // Формируем строку активных таймфреймов
        String active = settings.activeTimeframes.stream()
                .map(this::formatTimeframeDisplay)
                .collect(Collectors.joining(", "));

        StringBuilder text = new StringBuilder();
        text.append("Active timeframes:\n")
                .append(active.isEmpty() ? "None" : active)
                .append("\n\nSelect/Deselect Timeframes:");

        message.setText(text.toString());

        // Создание клавиатуры
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        int buttonsPerRow = 3;
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        for (int i = 0; i < settings.allTimeframes.size(); i++) {
            String timeframe = settings.allTimeframes.get(i);
            String buttonText = (settings.activeTimeframes.contains(timeframe) ? "✅ " : "") + formatTimeframeDisplay(timeframe);
            String callbackData = "timeframe_" + timeframe;

            currentRow.add(createInlineButton(buttonText, callbackData));

            if ((i + 1) % buttonsPerRow == 0 || i == settings.allTimeframes.size() - 1) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }

        // Добавляем кнопку возврата
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createInlineButton("🔙 Back to Main Menu", "back_to_main_menu"));
        rows.add(backRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод для форматирования отображения таймфрейма
    private String formatTimeframeDisplay(String timeframe) {
        switch (timeframe) {
            case "D": return "1d";
            case "W": return "1w";
            case "60": return "1h";
            case "120": return "2h";
            case "180": return "3h";
            case "240": return "4h";
            default: return timeframe + "m";
        }
    }

    private void sendTpSlMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        // Информация о текущих настройках TP и SL
        String infoMessage = "🎯 <b>Current Take Profit and Stop Loss Settings</b>\n\n" +
                "💰 <b>Take Profit:</b> " +
                (settings.tpType == 1 ? settings.tpPercent + "% (Fixed)" :
                        settings.tpType == 2 ? settings.tpAtr + " ATR" :
                                "Smart Levels (Recommended)") + "\n" +
                "🔻 <b>Stop Loss:</b> " +
                (settings.slType == 1 ? settings.slPercent + "% (Fixed)" :
                        settings.slType == 2 ? settings.slAtr + " ATR" :
                                "Smart Levels (Recommended)") + "\n\n" +
                "👉 <b>Select an option to change settings:</b>";

        message.enableHtml(true);
        message.setText(infoMessage);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для переключения метода TP
        List<InlineKeyboardButton> rowTpMethod = new ArrayList<>();
        rowTpMethod.add(createInlineButton("🎯 Toggle TP Method", "toggle_tp_method"));
        rows.add(rowTpMethod);

        // Кнопка для переключения метода SL
        List<InlineKeyboardButton> rowSlMethod = new ArrayList<>();
        rowSlMethod.add(createInlineButton("🔻 Toggle SL Method", "toggle_sl_method"));
        rows.add(rowSlMethod);

        // Кнопки для изменения значений TP
        List<InlineKeyboardButton> rowTp = new ArrayList<>();
        rowTp.add(createInlineButton("Set TP Percentage", "set_tp_percent"));
        rowTp.add(createInlineButton("Set TP ATR Multiplier", "set_tp_atr"));
        rows.add(rowTp);

        // Кнопки для изменения значений SL
        List<InlineKeyboardButton> rowSl = new ArrayList<>();
        rowSl.add(createInlineButton("Set SL Percentage", "set_sl_percent"));
        rowSl.add(createInlineButton("Set SL ATR Multiplier", "set_sl_atr"));
        rows.add(rowSl);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendBreakoutConfigMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        String infoMessage = "📐 <b>Breakout Configuration:</b>\n" +
                "- Pivot Period: " + settings.prd + "\n" +
                "- Threshold Rate: " + settings.thresholdRate + "\n" +
                "- Minimum Tests: " + settings.minTests + "\n" +
                "👉 <b>Choose a parameter to modify:</b>";

        message.enableHtml(true);
        message.setText(infoMessage);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Button to set Pivot Period
        List<InlineKeyboardButton> pivotRow = new ArrayList<>();
        InlineKeyboardButton pivotButton = new InlineKeyboardButton();
        pivotButton.setText("🔢 Set Pivot Period");
        pivotButton.setCallbackData("set_pivot_period");
        pivotRow.add(pivotButton);

        // Button to set Threshold Rate
        List<InlineKeyboardButton> thresholdRow = new ArrayList<>();
        InlineKeyboardButton thresholdButton = new InlineKeyboardButton();
        thresholdButton.setText("📉 Set Threshold Rate");
        thresholdButton.setCallbackData("set_threshold_rate");
        thresholdRow.add(thresholdButton);

        // Button to set Minimum Tests
        List<InlineKeyboardButton> testsRow = new ArrayList<>();
        InlineKeyboardButton testsButton = new InlineKeyboardButton();
        testsButton.setText("✔️ Set Minimum Tests");
        testsButton.setCallbackData("set_min_tests");
        testsRow.add(testsButton);

        // Back button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Back to Main Menu");
        backButton.setCallbackData("back_to_main_menu");
        backRow.add(backButton);

        // Add all rows to the menu
        rows.add(pivotRow);
        rows.add(thresholdRow);
        rows.add(testsRow);
        rows.add(backRow);
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSymbolWatchListMenu(long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        // Информация о текущих настройках TP и SL
        StringBuilder infoMessage = new StringBuilder("🎯 <b>Current Symbol WatchList:</b>\n\n");
        for (String symbol : tickersService.getTickers()){
            infoMessage.append(symbol +"\n");
        }
        infoMessage.append( "\n👉 <b>Select an option :</b>");

        message.enableHtml(true);
        message.setText(infoMessage.toString());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для переключения метода TP
        List<InlineKeyboardButton> addSymbol = new ArrayList<>();
        addSymbol.add(createInlineButton("Add symbol to watchlist", "add_symbol"));
        rows.add(addSymbol);

        // Кнопка для переключения метода SL
        List<InlineKeyboardButton> removeSymbol = new ArrayList<>();
        removeSymbol.add(createInlineButton("Remove symbol from watchlist", "remove_symbol"));
        rows.add(removeSymbol);

        // Кнопки для изменения значений TP
        List<InlineKeyboardButton> clearList = new ArrayList<>();
        clearList.add(createInlineButton("Clear watchlist", "clear_list"));
        rows.add(clearList);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendDetailedBFNotification(String symbol, String positionSide, String lastPrice,
                                           Double pivot, Double tp, Double sl, String timeframe, Double volume) throws TelegramApiException {
        double price = Double.parseDouble(lastPrice);

        double tpPercent = ((tp - price) / price) * 100;
        double slPercent = ((sl - price) / price) * 100;

        String notification = String.format(
                "%s position for %s on %s timeframe is opened at price %s.\n" +
                        "Key pivot: %.5f\n" +
                        "TP: %.5f (%.2f%%)\n" +
                        "SL: %.5f (%.2f%%)\n" +
                        "VolumeOsc: %.2f",
                positionSide, symbol, timeframe, lastPrice,
                pivot,
                tp, tpPercent,
                sl, slPercent,
                volume
        );

        SendMessage outMess = new SendMessage();
        outMess.setChatId(chatId);
        outMess.setText(notification);
        execute(outMess);
    }


    public void sendErrorNotification() throws TelegramApiException {
        String msg = "Error while fetching data";
        SendMessage outMess = new SendMessage();
        outMess.setChatId(chatId);

        outMess.setText(msg);
        execute(outMess);
    }

    public void sendSlChangeNotification(String symbol) throws TelegramApiException {
        String notification;
        notification = "Sl for " + symbol + " changed to break even";
        SendMessage outMess = new SendMessage();
        outMess.setChatId(chatId);

        outMess.setText(notification);
        execute(outMess);
    }
}
