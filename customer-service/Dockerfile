FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy root pom and module poms referenced by root pom (for dependency caching)
# POM-ы копируются отдельно → кэшируются зависимости.
COPY pom.xml .
COPY customer-service/pom.xml customer-service/pom.xml
COPY fraud-service/pom.xml fraud-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY exchange-service/pom.xml exchange-service/pom.xml
COPY exchange-generator-service/pom.xml exchange-generator-service/pom.xml
COPY front-ui-service/pom.xml front-ui-service/pom.xml
COPY apigw-service/pom.xml apigw-service/pom.xml
COPY cash-service/pom.xml cash-service/pom.xml
COPY transfer-service/pom.xml transfer-service/pom.xml
COPY clients/pom.xml clients/pom.xml

# Copy dependencies (to cache them) without src
RUN mvn -B -pl customer-service -am dependency:go-offline

# Copy the entire project
# Потом копируется весь проект → пересобирается только код.
COPY . .
# Build only customer-service and its dependencies
RUN mvn -B -pl customer-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/customer-service/target/customer-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
