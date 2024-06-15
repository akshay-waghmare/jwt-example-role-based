# Use an official OpenJDK runtime as a parent image
FROM openjdk:8-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the project’s jar file into the container at /app
COPY target/spring-security-jwt-1.0-SNAPSHOT.war /app/app.war

# Copy the H2 database file into the container
COPY testdb.mv.db /app/testdb.mv.db

# Expose port 8099 to the outside world
EXPOSE 8099

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.war"]
