FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY --from=build /workspace/target/zizu-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
USER nonroot:nonroot

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
