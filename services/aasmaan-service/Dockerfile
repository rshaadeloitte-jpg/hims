FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S hims && adduser -S hims -G hims
WORKDIR /app
COPY target/*.jar app.jar
USER hims:hims
EXPOSE 8112

ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
