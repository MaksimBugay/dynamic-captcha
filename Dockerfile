# Use a multi-stage build to reduce the final image size
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-25 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
# Use the Eclipse Temurin base image (non-Alpine)
FROM eclipse-temurin:25-jre

# Install curl for health check
RUN apt-get update && apt-get install -y curl && apt-get clean

# Set the working directory inside the container
WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Port the application listens on inside the container. Overrides server.port from the bundled application.yaml
# (8061, for running locally), so the health check and the stack's port mapping keep working.
ENV SERVER_PORT=8080
EXPOSE 8080

# Set environment variables for better performance and SecureRandom configuration
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/urandom"

# Add a health check using the Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]