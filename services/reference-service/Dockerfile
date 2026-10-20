# Multi-stage build: Maven builder -> Temurin 21 JRE, non-root, healthchecked.
# No secrets in the image (MIG-063); config via env vars (application.yml placeholders).

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S hims && adduser -S hims -G hims
WORKDIR /app
COPY --from=builder /build/target/reference-service-*.jar app.jar
USER hims:hims
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
