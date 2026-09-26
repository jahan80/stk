FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/

RUN mvn -f api-gateway/pom.xml dependency:go-offline -B

COPY api-gateway/src api-gateway/src

RUN mvn -f api-gateway/pom.xml clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S starterkit && adduser -S starterkit -G starterkit

COPY --from=builder /build/api-gateway/target/*.jar app.jar

USER starterkit

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
