# Use an official OpenJDK runtime as a parent image
FROM openjdk:24-ea-jdk-bullseye

RUN <<EOF
    apt-get -y update
    apt-get -y install wget curl bash unzip
EOF

# Set the working directory in the container
WORKDIR /app

# Download the executable jar file
# RUN wget https://github.com/sievericcardo/GreenHouseDT_API/releases/download/v0.2/greenhouse_api.jar

# Copy the executable jar file to the container
COPY greenhouse_api.jar /app/greenhouse_api.jar

# Download the smol folder
# RUN wget https://github.com/sievericcardo/GreenHouseDT_API/releases/download/v0.2/SMOL.zip

# Unzip the smol folder
# RUN unzip SMOL.zip

# Copy the smol folder
COPY src/main/resources/SMOL /app/SMOL

COPY config_local.yml /app/config_local.yml
COPY src/main/resources/watering-strategies.yml /app/watering-strategies.yml

# Expose the port that the application will run on
EXPOSE 8090

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/greenhouse_api.jar"]