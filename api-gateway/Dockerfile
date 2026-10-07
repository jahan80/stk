# ============================================================
# api-gateway — runtime-only Dockerfile
# ============================================================
# Build strategy:
#   The jar is built ON THE HOST by Maven (uses ~/.m2 cache, no
#   network dependency inside Docker).
#   Docker only packages the existing jar into a runtime image.
#
#   Build order:
#     1. mvn -pl api-gateway -am clean package -DskipTests   (on host)
#     2. docker compose up -d --build api-gateway
# ============================================================

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S starterkit && adduser -S starterkit -G starterkit

COPY api-gateway/target/*.jar app.jar

RUN chown -R starterkit:starterkit /app

USER starterkit

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
