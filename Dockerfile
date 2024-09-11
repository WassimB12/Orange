# Use an official Java runtime as a parent image
FROM openjdk:17-jdk-alpine

# The application's jar file
ARG JAR_FILE=target/Orange-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
COPY ${JAR_FILE} app.jar

# Expose the port that the app will run on
EXPOSE 8083

# Environment variables for MySQL connection
ENV MYSQL_HOST=localhost
ENV MYSQL_PORT=3307
ENV MYSQL_DATABASE=orangeDB
ENV MYSQL_USER=root

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app.jar"]
