FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (this layer can be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Add health check for container orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"] 