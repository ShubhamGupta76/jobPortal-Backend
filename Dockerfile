# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy only what's needed to resolve dependencies first, so this (slow) layer is cached and
# skipped on rebuilds that only change application source.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -q -DskipTests dependency:go-offline

# Now copy the actual source and build the jar.
COPY src ./src
RUN ./mvnw -q -DskipTests clean package \
    && mv target/*.jar target/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user rather than the image's default root.
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build --chown=spring:spring /app/target/app.jar app.jar
USER spring

# Render sets PORT at runtime; application.properties already reads it via ${PORT:8080}.
# This EXPOSE is documentation only (Render ignores it and routes to $PORT directly).
EXPOSE 8080

# SPRING_PROFILES_ACTIVE is set on Render (e.g. "prod") as an environment variable, not baked
# into the image, so the same image works for any profile depending on how it's deployed.
# JAVA_OPTS is overridable per-deployment; the default keeps the JVM heap within whatever
# container memory limit Render assigns instead of the JVM guessing from host memory.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
