FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml ./pom.xml
COPY met-platform/pom.xml ./met-platform/pom.xml
COPY met-platform/src ./met-platform/src

RUN mvn -f pom.xml -pl met-platform -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/met-platform/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
