# Build context is the repository root (see docker-compose.yml), because
# this module depends on sibling modules in the Maven reactor (e.g. common,
# security-service, iso8583-service) that live outside this directory and
# aren't published anywhere - a per-module build context can't see them.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl issuer-service -am -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/issuer-service/target/issuer-service-1.0.0.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1
