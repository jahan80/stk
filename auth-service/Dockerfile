# ==================================================
# Stage 1: Build
# ==================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy parent pom
COPY pom.xml .

# Copy auth-service pom
COPY auth-service/pom.xml auth-service/

# Download dependencies (cache layer)
RUN mvn -f auth-service/pom.xml dependency:go-offline -B

# Copy source
COPY auth-service/src auth-service/src

# Build
RUN mvn -f auth-service/pom.xml clean package -DskipTests -B


# ==================================================
# Stage 2: Runtime
# ==================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S starterkit && adduser -S starterkit -G starterkit

# Copy JAR
COPY --from=builder /build/auth-service/target/*.jar app.jar

# Copy JWT keys (mounted as volume in compose, but include as fallback)
# Note: In production, use secrets management

USER starterkit

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
