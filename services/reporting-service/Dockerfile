FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S hims && adduser -S hims -G hims
WORKDIR /app
COPY target/*.jar app.jar
USER hims:hims
EXPOSE 8106
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD wget -qO- http://localhost:8106/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
