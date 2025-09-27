FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Копируем родительский POM и POM модуля
COPY pom.xml .
COPY customer-service/pom.xml customer-service/pom.xml

# Скачиваем зависимости
RUN mvn dependency:go-offline -pl customer-service -am

# Копируем исходный код модуля
COPY customer-service/src customer-service/src

# Собираем проект
RUN mvn package -pl customer-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/customer-service/target/customer-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
