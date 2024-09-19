FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/*.jar /app/Orange.jar

# Expose the port that the application runs on
EXPOSE 8089

# Define the command to run the JAR file
ENTRYPOINT ["java", "-jar", "Orange.jar"]
