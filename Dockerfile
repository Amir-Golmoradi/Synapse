# syntax=docker/dockerfile:1

# ─── Stage 1: Dependency Cache ────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS dependencies

WORKDIR /app

COPY --chmod=0755 mvnw ./mvnw
COPY .mvn/ .mvn/
COPY pom.xml ./pom.xml

RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline


# ─── Stage 2: Application Build ───────────────────────────────────────────────
FROM dependencies AS builder

WORKDIR /app

COPY config/ ./config/
COPY src/ ./src/

RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests


# ─── Stage 3: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S synapse \
    && adduser -S synapse -G synapse

COPY --from=builder --chown=synapse:synapse /app/target/*.jar ./app.jar

USER synapse

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]