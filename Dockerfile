# ---- Build stage: compile and package the Spring Boot jar ----
FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Tests need a live PostgreSQL, which isn't available at image-build time -> skip them.
RUN mvn -B clean package -DskipTests

# ---- Runtime stage: slim JRE with just the jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
