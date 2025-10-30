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
* Kotlin + JDK 21
* Ktor HTTP Server - [Ktor](https://ktor.io/)
* Dependency Injection - [Kodein](https://github.com/Kodein-Framework/Kodein-DI)
* Build system - [Gradle](https://gradle.org/)
* Communication with [Wire Applications JVM SDK](https://github.com/wireapp/wire-apps-jvm-sdk)

## Usage

* The app needs Postgres database up & running - we use one in [docker-compose.yml](docker-compose.yml), to start it up, you can use
  command `make db`.
* To run the application execute `make run` or `./gradlew run`.
* To run the application inside the docker compose environment run `make up`.

For more details see [Makefile](Makefile).

## App configuration
Configuration is currently being loaded from the environment variables.

Via the system variables - see [complete list](src/main/kotlin/com/wire/apps/polls/setup/EnvConfigVariables.kt).
And also the env variables required by `Wire Applications JVM SDK`

## Docker Compose
To run app inside docker compose environment with default PostgreSQL database,
please create `.env` file in the root directory, starting from the .env.sample file.
