FROM adoptopenjdk/openjdk11:alpine AS build
LABEL description="Wire Poll App"
LABEL project="wire-apps:polls"

ENV PROJECT_ROOT /src
WORKDIR $PROJECT_ROOT

# Copy gradle settings
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew $PROJECT_ROOT/
# Make sure gradlew is executable
RUN chmod +x gradlew
# Copy gradle specification
COPY gradle $PROJECT_ROOT/gradle
# Download gradle
RUN ./gradlew --version
# download and cache dependencies
RUN ./gradlew resolveDependencies --no-daemon

# Copy project and build
COPY . $PROJECT_ROOT
RUN ./gradlew distTar --no-daemon

# Runtime
FROM adoptopenjdk/openjdk11:alpine-jre

ENV APP_ROOT /app
WORKDIR $APP_ROOT

# Obtain built from the base
COPY --from=build /src/build/distributions/app.tar $APP_ROOT/

# Extract executables
RUN mkdir $APP_ROOT/run
RUN tar -xvf app.tar --strip-components=1 -C $APP_ROOT/run

# ------------------- Wire common -----------------
# create version file
ARG release_version=development
ENV RELEASE_FILE_PATH=$APP_ROOT/run/release.txt
RUN echo $release_version > $RELEASE_FILE_PATH
# enable json logging
# TODO enable this once we fully migrate to JSON logging everywhere
# ENV JSON_LOGGING=true
# /------------------ Wire common -----------------

EXPOSE 8080
ENTRYPOINT ["/bin/sh", "-c", "/app/run/bin/polls"]
