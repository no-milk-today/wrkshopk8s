FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy root pom and module pom — for dependency caching
COPY pom.xml .
COPY customer-service/pom.xml customer-service/pom.xml

# Copy dependencies (to cache them) without src
RUN mvn -B -pl customer-service -am dependency:go-offline

# Copy the entire project
COPY . .

# Build only customer-service and its dependencies
RUN mvn -B -pl customer-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/customer-service/target/customer-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
