# 构建阶段
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src

RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 1001 chatbi && \
    useradd --uid 1001 --gid 1001 --system --create-home --shell /usr/sbin/nologin chatbi

WORKDIR /app

COPY --from=builder /app/target/chatbi-server-*.jar app.jar

RUN mkdir -p /app/logs /app/uploads && \
    chown -R chatbi:chatbi /app

USER chatbi

EXPOSE 8081

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"
ENV SERVER_PORT=8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -jar app.jar"]
