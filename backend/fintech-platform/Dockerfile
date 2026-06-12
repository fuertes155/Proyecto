FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml ./pom.xml
COPY fintech-platform/pom.xml ./fintech-platform/pom.xml
COPY fintech-platform/src ./fintech-platform/src

RUN mvn -f pom.xml -pl fintech-platform -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/fintech-platform/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
