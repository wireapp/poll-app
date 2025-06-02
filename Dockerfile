FROM gradle:8.7-jdk17 AS build
LABEL description="Wire Poll App"
LABEL project="wire-apps:polls"

WORKDIR /setup

COPY . .

RUN gradle shadowJar

# Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat jar from the build stage
COPY --from=build /setup/build/libs/poll-app*.jar /app/app.jar

ENV JSON_LOGGING=true

# Run the application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
