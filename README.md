# Wire Poll App
[![GitHub version](https://badge.fury.io/gh/wireapp%2Fpoll-app.svg)](https://badge.fury.io/gh/wireapp%2Fpoll-app)
[![Build](https://github.com/wireapp/poll-app/actions/workflows/pull-request.yml/badge.svg)](https://github.com/wireapp/poll-app/actions/workflows/pull-request.yml)

[Wire](https://wire.com/) app for the polls.
Based on [Wire Applications JVM SDK](https://github.com/wireapp/wire-apps-jvm-sdk), which offers full support for MLS.  
Previous [Poll Bot](https://github.com/wireapp/poll-bot/) implementation was based on [Roman](https://github.com/wireapp/roman) therefore did not employ E2EE directly, and worked with Proteus protocol only.

## Commands
Basic usage 
* `/poll "Question" "Option 1" "Option 2"` will create poll
* `/poll help` to show help
* `/poll version` prints the current version of the poll app

## Technologies used
* HTTP Server for Prometheus - [Ktor](https://ktor.io/)
* Dependency Injection - [Kodein](https://github.com/Kodein-Framework/Kodein-DI)
* Build system - [Gradle](https://gradle.org/)
* Communication with [Wire Applications JVM SDK](https://github.com/wireapp/wire-apps-jvm-sdk)

## Usage

* The app needs Postgres database up & running - we use one in [docker-compose.yml](docker-compose.yml), to start it up, you can use
  command `make db`.
* To run the application execute `make run` or `./gradlew run`.
* To run the application inside the docker compose environment run `make up`.

For more details see [Makefile](Makefile).

## Docker Images

Poll app has public [docker image](https://quay.io/wire/poll-bot).
```bash
quay.io/wire/poll-bot
```

Tag `latest` is the latest release. [Releases](https://github.com/wireapp/poll-bot/releases) have then images with corresponding tag, so you
can always roll back. Tag `staging` is build from the latest commit in `staging` branch.


## App configuration
Configuration is currently being loaded from the environment variables.

```kotlin
    /**
     * Username for the database.
     */
    const val DB_USER = "DB_USER"

    /**
     * Password for the database.
     */
    const val DB_PASSWORD = "DB_PASSWORD"

    /**
     * URL for the database.
     *
     * Example:
     * `jdbc:postgresql://localhost:5432/app-database`
     */
    const val DB_URL = "DB_URL"

```

Via the system variables - see [complete list](src/main/kotlin/com/wire/apps/polls/setup/EnvConfigVariables.kt).

## Docker Compose
To run app inside docker compose environment with default PostgreSQL database,
please create `.env` file in the root directory with the following variables:
```bash
# database
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_DB=

# application
DB_USER=
DB_PASSWORD=
DB_URL=

```

Such configuration can look for example like that:

```bash
# database
POSTGRES_USER=wire-poll-app
POSTGRES_PASSWORD=super-secret-wire-pwd
POSTGRES_DB=poll-app

# application
DB_USER=wire-poll-app
DB_PASSWORD=super-secret-wire-pwd
DB_URL=jdbc:postgresql://db:5432/poll-app

```
