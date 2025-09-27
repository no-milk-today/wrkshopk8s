FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Копируем родительский POM и POM модуля
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml

# Скачиваем зависимости
RUN mvn dependency:go-offline -pl order-service -am

# Копируем исходный код модуля
COPY order-service/src order-service/src

# Собираем проект
RUN mvn package -pl order-service -am -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/order-service/target/order-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
