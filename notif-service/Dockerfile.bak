FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY notif-service/pom.xml notif-service/

RUN mvn -f notif-service/pom.xml dependency:go-offline -B

COPY notif-service/src notif-service/src

RUN mvn -f notif-service/pom.xml clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S starterkit && adduser -S starterkit -G starterkit

COPY --from=builder /build/notif-service/target/*.jar app.jar

USER starterkit

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
