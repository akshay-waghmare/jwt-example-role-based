# Use an official OpenJDK runtime as a parent image
FROM openjdk:8-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the project’s jar file into the container at /app
COPY target/spring-security-jwt-1.0-SNAPSHOT.war /app/app.war

# Expose port 8099 to the outside world
EXPOSE 8099

# Run the application
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "/app/app.war"]

