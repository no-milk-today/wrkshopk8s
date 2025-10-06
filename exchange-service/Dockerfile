FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY exchange-service/pom.xml exchange-service/pom.xml
COPY exchange-generator-service/pom.xml exchange-generator-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY customer-service/pom.xml customer-service/pom.xml
COPY fraud-service/pom.xml fraud-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY clients/pom.xml clients/pom.xml

RUN mvn -B -pl exchange-service -am dependency:go-offline

COPY . .

RUN mvn -B -pl exchange-service -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/exchange-service/target/exchange-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
