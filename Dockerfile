# Build the Spring Boot application with Java 21.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

# Run the packaged application on a lightweight Java 21 runtime.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --system --uid 10001 --user-group --create-home spring \
    && mkdir -p /app/data/videos /app/data/photos \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

ENV VIDEO_STORAGE_DIR=/app/data/videos \
    PHOTO_STORAGE_DIR=/app/data/photos

VOLUME ["/app/data/videos", "/app/data/photos"]
EXPOSE 8080
USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]