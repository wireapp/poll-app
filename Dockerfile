FROM gradle:8.4.0-jdk21 AS build
LABEL description="Wire Poll App"
LABEL project="wire-apps:polls"

WORKDIR /setup

COPY . ./

RUN ./gradlew clean shadowJar --no-daemon

# Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the fat jar from the build stage
COPY --from=build /setup/build/libs/poll-app*.jar /app/app.jar

RUN mkdir -p storage

# Run the application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
