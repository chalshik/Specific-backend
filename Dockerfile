FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .
COPY system.properties .

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the jar file
COPY --from=build /app/target/*.jar app.jar

# Create a script that uses the PORT environment variable and optimizes memory
RUN echo '#!/bin/sh' > /app/entrypoint.sh && \
    echo 'echo "Starting app on port ${PORT:=8080}"' >> /app/entrypoint.sh && \
    echo 'JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAM=512m -XX:+UseSerialGC -Xss512k}"' >> /app/entrypoint.sh && \
    echo 'echo "Using Java options: ${JAVA_OPTS}"' >> /app/entrypoint.sh && \
    echo 'exec java ${JAVA_OPTS} -Dserver.port=${PORT} -Dspring.profiles.active=prod -jar /app/app.jar' >> /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh

# Expose port (this is just documentation, the app will use $PORT)
EXPOSE 8080

# Run the application
ENTRYPOINT ["/app/entrypoint.sh"] 