# Use an appropriate base image for Java
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Accept build argument for the JAR file
ARG JAR_FILE=target/*.jar

# Copy the JAR file into the container
COPY ${JAR_FILE} /app/Orange.jar

# Expose the port that the application runs on
EXPOSE 8083

# Define the command to run the JAR file
ENTRYPOINT ["java", "-jar", "Orange.jar"]