# Pinned by digest, not just tag: a tag is a mutable pointer, so a rebuild could
# silently pick up different bytes. Re-pin when intentionally upgrading the base:
#   docker pull eclipse-temurin:<tag>
#   docker inspect --format='{{index .RepoDigests 0}}' eclipse-temurin:<tag>
FROM eclipse-temurin:25.0.2_10-jre-noble@sha256:a051234f864d7ab78bf0188c3c540ac06c711a3b566f00f246be37073cc99dce

# Install curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Non-root user
RUN groupadd spring && useradd -g spring spring
USER spring:spring

WORKDIR /app

# Expects the JAR to be prebuilt by `./mvnw package` (or `verify`) before `docker build`.
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

# 127.0.0.1, not localhost: that also resolves to ::1, and the JVM listens on
# IPv4 only. start-period covers a ~45s cold boot on a 1-vCPU host.
HEALTHCHECK --interval=2m --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
