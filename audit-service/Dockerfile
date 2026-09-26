FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY audit-service/pom.xml audit-service/

RUN mvn -f audit-service/pom.xml dependency:go-offline -B

COPY audit-service/src audit-service/src

RUN mvn -f audit-service/pom.xml clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S starterkit && adduser -S starterkit -G starterkit

COPY --from=builder /build/audit-service/target/*.jar app.jar

USER starterkit

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
