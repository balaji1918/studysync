FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# MySQL integration tests require Docker and run before merging, outside image builds.
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system studysync && useradd --system --gid studysync studysync
COPY --from=build --chown=studysync:studysync /build/target/studysync-0.1.0.jar /app/app.jar
USER studysync
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
