FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY fraudr-service/pom.xml fraudr-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY clients/pom.xml clients/pom.xml

RUN mvn -B -pl fraudr-service -am dependency:go-offline

COPY . .
RUN mvn -B -pl fraudr-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/fraudr-service/target/fraudr-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
