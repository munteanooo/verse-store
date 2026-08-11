# syntax=docker/dockerfile:1

FROM eclipse-temurin:26-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -DskipTests \
    -Dproject.build.outputTimestamp=2026-01-01T00:00:00Z \
    clean package

FROM eclipse-temurin:26-jre-alpine AS runtime

LABEL org.opencontainers.image.title="Verse Store" \
      org.opencontainers.image.description="Minimalist product catalog and local administration interface" \
      org.opencontainers.image.vendor="Verse"

RUN apk upgrade --no-cache \
    && addgroup -S verse \
    && adduser -S -D -H -G verse -u 10001 verse

WORKDIR /app
COPY --from=build --chown=verse:verse /workspace/target/verse-store-*.jar app.jar

USER verse:verse
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
