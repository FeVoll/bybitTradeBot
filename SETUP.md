# Инструкция по настройке Bybit Auto Trader

## 🚀 Быстрый старт

### 1. Подготовка окружения

Убедитесь, что у вас установлены:
- Java 17+
- Maven 3.6+
- PostgreSQL 12+

### 2. Настройка базы данных

```sql
-- Создайте базу данных
CREATE DATABASE crypto;

-- Создайте пользователя (опционально)
CREATE USER trader_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE crypto TO trader_user;
```

### 3. Получение API ключей

#### Bybit API
1. Зарегистрируйтесь на [Bybit](https://www.bybit.com/)
2. Перейдите в API Management
3. Создайте новый API ключ с правами:
   - Read (обязательно)
   - Trade (для торговли)
   - Derivatives (для фьючерсов)
4. **Важно**: Начните с демо-счета для тестирования!

#### Telegram Bot
1. Найдите [@BotFather](https://t.me/botfather) в Telegram
2. Создайте нового бота командой `/newbot`
3. Следуйте инструкциям и получите токен
4. Узнайте свой Chat ID:
   - Напишите боту [@userinfobot](https://t.me/userinfobot)
   - Или добавьте бота в группу и используйте ID группы

### 4. Настройка конфигурации

#### Создание конфигурационных файлов

```bash
# Скопируйте шаблоны
cp src/main/resources/application-dev-template.yml src/main/resources/application-dev.yml
cp src/main/resources/application-prod-template.yml src/main/resources/application-prod.yml
```

#### Настройка application-dev.yml (для тестирования)

```yaml
bot:
  name: YourBotName
  token: YOUR_TELEGRAM_BOT_TOKEN
  chatId: YOUR_TELEGRAM_CHAT_ID

bybit:
  apiKey: YOUR_BYBIT_API_KEY
  apiSecret: YOUR_BYBIT_API_SECRET
  url: https://api-demo.bybit.com  # Демо-счет
```

#### Настройка application-prod.yml (для продакшена)

```yaml
bot:
  name: YourBotName
  token: YOUR_TELEGRAM_BOT_TOKEN
  chatId: YOUR_TELEGRAM_CHAT_ID

bybit:
  apiKey: YOUR_BYBIT_API_KEY
  apiSecret: YOUR_BYBIT_API_SECRET
  apiKeyS: YOUR_SECONDARY_API_KEY  # Опционально
  apiSecretS: YOUR_SECONDARY_API_SECRET  # Опционально
  url: https://api.bybit.com  # Реальный счет
```

#### Настройка application.yml

```yaml
server:
  port: 8050

spring:
  datasource:
    driver-class: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/crypto
    username: your_db_username
    password: your_db_password
  jpa:
    hibernate:
      ddl-auto: update
  profiles:
    active: dev  # Измените на prod для продакшена
```

### 5. Использование переменных окружения (рекомендуется)

Создайте файл `.env` в корне проекта:

```bash
# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Telegram
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
TELEGRAM_CHAT_ID=your_telegram_chat_id

# Bybit
BYBIT_API_KEY=your_bybit_api_key
BYBIT_API_SECRET=your_bybit_api_secret
BYBIT_API_KEY_S=your_secondary_api_key
BYBIT_API_SECRET_S=your_secondary_api_secret
```

**Важно**: Добавьте `.env` в `.gitignore`!

### 6. Запуск приложения

#### Режим разработки
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Режим продакшена
```bash
mvn clean package
java -jar target/bybitAutoTrader-2.0.0-Release.jar --spring.profiles.active=prod
```

#### С переменными окружения
```bash
export $(cat .env | xargs) && mvn spring-boot:run
```

### 7. Проверка работы

1. Запустите приложение
2. Откройте Telegram и найдите своего бота
3. Отправьте любое сообщение боту
4. Должно появиться главное меню с настройками

## ⚙️ Настройка торговых параметров

### Основные настройки

- **Position Value**: Размер позиции в USDT (без учета плеча)
- **Leverage**: Плечо для торговли (1x-100x)
- **Active Trades**: Максимальное количество открытых позиций
- **Timeframes**: Таймфреймы для анализа (1m, 5m, 15m, 30m, 1h, 2h, 3h, 4h, 1d)

### Алгоритмы прорывов

- **Pivot Period**: Период для расчета пивотных точек (рекомендуется 20-50)
- **Threshold Rate**: Пороговая ширина прорыва (0.05-0.15)
- **Minimum Tests**: Минимальное количество тестов уровня (1-5)
- **ATR Multiplier**: Множитель ATR для уровней (2-6)

### Управление рисками

- **Take Profit**: 
  - Fixed %: Фиксированный процент прибыли
  - ATR: На основе Average True Range
  - Smart Levels: Умные уровни (рекомендуется)
  
- **Stop Loss**:
  - Fixed %: Фиксированный процент убытка
  - ATR: На основе Average True Range
  - Smart Levels: Умные уровни (рекомендуется)

- **Safe Mode**: Дополнительные проверки для безопасности

## 🔒 Безопасность

### Рекомендации

1. **Начните с демо-счета** - тестируйте все настройки без риска
2. **Используйте малые суммы** - начинайте с минимальных позиций
3. **Настройте IP-ограничения** для API ключей
4. **Регулярно ротируйте** API ключи
5. **Мониторьте логи** на предмет ошибок

### Права API ключей

Минимально необходимые права:
- **Read**: Чтение данных рынка
- **Trade**: Размещение ордеров
- **Derivatives**: Торговля фьючерсами

**НЕ включайте**:
- Withdraw (вывод средств)
- Transfer (переводы)

## 🐛 Устранение неполадок

### Частые проблемы

#### Ошибка подключения к БД
```
Connection refused
```
**Решение**: Убедитесь, что PostgreSQL запущен и доступен

#### Ошибка API Bybit
```
Invalid API key
```
**Решение**: Проверьте правильность API ключей и их права

#### Telegram бот не отвечает
**Решение**: 
1. Проверьте токен бота
2. Убедитесь, что бот запущен
3. Проверьте Chat ID

#### Ошибки торговли
```
Insufficient balance
```
**Решение**: Убедитесь, что на счете достаточно средств

### Логи

Проверьте логи для диагностики:
```bash
# Логи приложения
tail -f logs/binance-java-connector.log

# Логи Spring Boot
# В консоли при запуске
```

## 📊 Мониторинг

### Telegram уведомления

Бот отправляет уведомления о:
- Открытии новых позиций
- Изменении стоп-лоссов
- Ошибках получения данных
- Статусе работы бота

### Рекомендуемый мониторинг

1. **Регулярно проверяйте** статус бота
2. **Мониторьте открытые позиции**
3. **Следите за уведомлениями** об ошибках
4. **Проверяйте логи** на предмет проблем

## ⚠️ Важные предупреждения

1. **Торговля криптовалютами связана с высокими рисками**
2. **Всегда тестируйте на демо-счетах**
3. **Используйте только те средства, которые можете позволить себе потерять**
4. **Автор не несет ответственности за возможные потери**
5. **Регулярно делайте резервные копии настроек**

