# Use an official Java runtime as a parent image
FROM openjdk:17-jdk-alpine

ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# The application's jar file
ARG JAR_FILE=target/Orange-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
COPY ${JAR_FILE} app.jar

# Expose the port that the app will run on
EXPOSE 8083

# Environment variables for MySQL connection


# Run the jar file
ENTRYPOINT ["java", "-Dserver.port=8089", "-jar", "/app.jar"]
