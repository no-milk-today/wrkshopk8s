FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY fraud-service/pom.xml fraud-service/pom.xml
COPY clients/pom.xml clients/pom.xml

RUN mvn -B -pl fraud-service -am dependency:go-offline

COPY . .
RUN mvn -B -pl fraud-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/fraud-service/target/fraud-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
