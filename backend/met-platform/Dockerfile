FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml ./pom.xml
COPY met-platform/pom.xml ./met-platform/pom.xml
COPY met-platform/src ./met-platform/src

RUN mvn -f pom.xml -pl met-platform -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# No correr como root: crea un usuario/grupo de sistema dedicado y ejecuta la app con él.
RUN addgroup -S met && adduser -S met -G met
COPY --from=builder --chown=met:met /build/met-platform/target/*.jar app.jar
USER met

EXPOSE 8080

# Flags de memoria/JIT afinados para instancias chicas (Render free = 512 MB):
# heap ~50% del contenedor, SerialGC (menos overhead que G1 en heaps chicos),
# metaspace y code-cache acotados, JIT solo C1 (menos RAM y arranque más rápido).
# $JAVA_OPTS al final permite sobreescribir; en Render conviene NO definir
# JAVA_OPTS para que estos valores manden.
ENV JAVA_TUNING="-XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC -XX:MaxMetaspaceSize=140m -XX:ReservedCodeCacheSize=40m -XX:TieredStopAtLevel=1 -Xss384k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false"
ENTRYPOINT ["sh", "-c", "java $JAVA_TUNING $JAVA_OPTS -jar app.jar"]
