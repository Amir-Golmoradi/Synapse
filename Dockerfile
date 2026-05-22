# ─── Stage 1: Dependency Cache Stage ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS cache-builder

WORKDIR /app

# Copy wrapper files and parent config
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy all submodule descriptors to allow dependency mapping
# (Matches multi-module directory structures)
COPY identity/pom.xml ./identity/
COPY shared/pom.xml ./shared/

# Cache all remote repository artifacts cleanly
RUN ./mvnw dependency:go-offline --no-transfer-progress

# ─── Stage 2: Application Compilation Stage ──────────────────────────────────
FROM cache-builder AS compilation-builder

WORKDIR /app

# Copy complete local source directories
COPY identity/src ./identity/src
COPY shared/src ./shared/src

# Package the absolute multi-module binary without executing tests
RUN ./mvnw package -DskipTests --no-transfer-progress

# ─── Stage 3: Lightweight Runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Security isolation: Create and run under a limited execution profile
RUN addgroup -S synapse && adduser -S synapse -G synapse
USER synapse

# Copy the final executable fat JAR artifact directly from your primary entry point module
# Adjust this path to target whichever module serves as your main @SpringBootApplication container
COPY --from=compilation-builder /app/identity/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]