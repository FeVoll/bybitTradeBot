# Используем официальный образ OpenJDK 17
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем Maven wrapper и pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Даем права на выполнение Maven wrapper
RUN chmod +x ./mvnw

# Скачиваем зависимости (для кэширования)
RUN ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY src src

# Собираем приложение
RUN ./mvnw clean package -DskipTests

# Создаем директорию для логов
RUN mkdir -p /app/logs

# Открываем порт
EXPOSE 8050

# Запускаем приложение
CMD ["java", "-jar", "target/bybitAutoTrader-2.0.0-Release.jar"]
