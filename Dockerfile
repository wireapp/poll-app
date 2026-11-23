FROM gradle:8.14.3-jdk21 AS build
LABEL description="Wire Poll App"
LABEL project="wire-apps:polls"

WORKDIR /setup

COPY . ./

RUN ./gradlew clean shadowJar --no-daemon

# Runtime
FROM eclipse-temurin:21-jre

# Workaround for CVE-2026-114938 (Java4Shell, WPB-30191): disable the AVX-512 Base64 intrinsic.
# Remove once the image runs Temurin >= 21.0.13.1.
ENV JAVA_TOOL_OPTIONS="-XX:-UseBASE64Intrinsics"

WORKDIR /app

# Copy the fat jar from the build stage
COPY --from=build /setup/build/libs/poll-app*.jar /app/app.jar

RUN mkdir -p storage

# Run the application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
