# ---- Build stage: compiles, runs the tests and packages the jar ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B package

# ---- Runtime stage: slim JRE image, non-root user ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/data && chown -R app:app /app
USER app

COPY --from=build /workspace/target/animal-picture-app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
