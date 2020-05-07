# Wire Poll Bot
[![GitHub version](https://badge.fury.io/gh/wireapp%2Fpoll-bot.svg)](https://badge.fury.io/gh/wireapp%2Fpoll-bot)
![CI](https://github.com/wireapp/poll-bot/workflows/CI/badge.svg)
![Test pipeline](https://github.com/wireapp/poll-bot/workflows/Test%20pipeline/badge.svg)
![Docker Latest build](https://github.com/wireapp/poll-bot/workflows/Docker%20Latest%20build/badge.svg)
![Staging Deployment](https://github.com/wireapp/poll-bot/workflows/Staging%20Deployment/badge.svg)
![Release Pipeline](https://github.com/wireapp/poll-bot/workflows/Release%20Pipeline/badge.svg)

[Wire](https://wire.com/) bot for the polls.
Service code to enable Poll bot in your team:
```bash
3131a0af-89d4-4569-b36d-50bcced4b1fb:163a422b-c314-4e34-95af-10b6b36cde18
```

## Commands
Basic usage 
* `/poll "Question" "Option 1" "Option 2"` will create poll
* `/stats` will send result of the last poll in the conversation
* `/help` to show help


## Dev Stack
* HTTP Server - [Ktor](https://ktor.io/)
* HTTP Client - [Apache](https://ktor.io/clients/http-client/engines.html) under [Ktor](https://ktor.io/)
* Dependency Injection - [Kodein](https://github.com/Kodein-Framework/Kodein-DI)
* Build system - [Gradle](https://gradle.org/)
* Communication with [Wire](https://wire.com/) - [Roman](https://github.com/dkovacevic/roman)

Bot can connect to web socket stream or can use webhook from Roman.

## Usage
* To run the application simply execute `make run` or `./gradlew run`.
* To run the application inside the docker compose environment run `make up`

For more details see [Makefile](Makefile).


## Docker Images
Poll bot has public [docker image](https://hub.docker.com/r/lukaswire/polls).
```bash
lukaswire/polls
```
Tag `latest` is current master branch - each commit is build and tagged as `latest`.
[Releases](https://github.com/wireapp/poll-bot/releases) have then images with corresponding tag.


## Bot configuration
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
     * `jdbc:postgresql://localhost:5432/bot-database`
     */
    const val DB_URL = "DB_URL"

    /**
     * Token which is used for the auth of proxy.
     */
    const val SERVICE_TOKEN = "SERVICE_TOKEN"
    /**
     * Key for connecting to the web socket of the proxy.
     */
    const val APP_KEY = "APP_KEY"
    /**
     * Domain used for sending the messages from the bot to proxy eg. "https://proxy.services.zinfra.io"
     */
    const val PROXY_DOMAIN = "PROXY_DOMAIN"
```

Via the system variables - see [complete list](src/main/kotlin/com/wire/bots/polls/setup/EnvConfigVariables.kt).

## Docker Compose
To run bot inside docker compose environment with default PostgreSQL database,
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
SERVICE_TOKEN=
APP_KEY=
PROXY_DOMAIN=
```
