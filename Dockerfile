# Multi-stage build für OpenFamilyCompass

# Stage 1: Build Stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Kopiere Maven-Konfiguration für besseres Caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kopiere den Quellcode
COPY src ./src

# Build der Anwendung (skippt Tests für schnelleren Build)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime Stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Erstelle einen non-root User für bessere Sicherheit
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Kopiere das JAR aus der Build Stage
COPY --from=build /app/target/*.jar app.jar

# Exponiere den Port
EXPOSE 8080

# Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Starte die Anwendung
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
