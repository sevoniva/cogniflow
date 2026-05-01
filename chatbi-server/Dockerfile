FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/chatbi-server-2.0.0-ENTERPRISE.jar app.jar

RUN mkdir -p /app/logs /app/uploads

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SERVER_PORT=8081

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -jar app.jar"]
