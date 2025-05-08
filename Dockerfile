FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (this layer can be cached)
RUN mvn dependency:go-offline -B

# Copy source code and startup script
COPY src ./src
COPY startup.sh ./startup.sh

# Build the application
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy startup script from build stage
COPY --from=build /app/startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

# Add health check for container orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Expose the port for the application
EXPOSE 8080

# Run the application using the startup script
ENTRYPOINT ["/app/startup.sh"] 