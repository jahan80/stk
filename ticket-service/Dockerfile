FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S starterkit && adduser -S starterkit -G starterkit
COPY ticket-service/target/*.jar app.jar
RUN chown -R starterkit:starterkit /app
USER starterkit
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
