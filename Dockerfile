
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY .mvn/settings-docker.xml /root/.m2/settings.xml
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2/repository mvn -s /root/.m2/settings.xml -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn -s /root/.m2/settings.xml -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN mkdir -p /var/lib/official-website/media
COPY --from=builder /workspace/target/official-website-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
