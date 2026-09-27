FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S starterkit && adduser -S starterkit -G starterkit

COPY notif-service/target/*.jar app.jar

USER starterkit

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
