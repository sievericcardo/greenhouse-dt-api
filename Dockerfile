# Use an official OpenJDK runtime as a parent image
FROM openjdk:24-oraclelinux8

# Set the working directory in the container
WORKDIR /app

# Copy the executable jar file to the container
COPY greenhouse_api.jar /app/greenhouse_api.jar

# Copy the smol file
COPY SMOL /app/SMOL

# Expose the port that the application will run on
EXPOSE 8090

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/greenhouse_api.jar"]