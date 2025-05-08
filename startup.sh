#!/bin/bash
echo "Starting application on port ${PORT:-8080}..."
java -Dspring.profiles.active=prod -jar /app/app.jar 