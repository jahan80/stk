FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S starterkit && adduser -S starterkit -G starterkit
COPY auth-service/target/*.jar app.jar
USER starterkit
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
