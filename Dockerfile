# syntax=docker/dockerfile:1.7

############################
# Stage 1: build
############################
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -e -ntp -DskipTests package

RUN mkdir -p /workspace/extracted \
 && cp target/backend-0.0.1-SNAPSHOT.jar /workspace/extracted/app.jar \
 && cd /workspace/extracted \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher \
 && rm app.jar

############################
# Stage 2: runtime
############################
FROM eclipse-temurin:25-jdk-alpine AS runtime

RUN apk add --no-cache curl \
 && addgroup -S spring \
 && adduser -S -G spring -H -s /sbin/nologin spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/extracted/dependencies/          ./
COPY --from=build --chown=spring:spring /workspace/extracted/spring-boot-loader/    ./
COPY --from=build --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/application/           ./

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
