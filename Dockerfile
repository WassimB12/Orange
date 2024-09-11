# Stage 1: Build the application
FROM maven:3.8.4-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/Orange-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
