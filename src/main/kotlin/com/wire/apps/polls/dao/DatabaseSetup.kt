package com.wire.apps.polls.dao

import com.wire.apps.polls.dto.conf.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Object with methods for managing the database.
 */
object DatabaseSetup {
    /**
     * Connect app to the database via provided credentials.
     *
     * This method does not check whether it is possible to connect to the database, use [isConnected] for that.
     */
    fun connect(dbConfiguration: DatabaseConfiguration) =
        Database.connect(
            url = dbConfiguration.url,
            user = dbConfiguration.userName,
            password = dbConfiguration.password,
            driver = "org.postgresql.Driver"
        )

    /**
     * Returns true if the app is connected to database
     */
    fun isConnected() =
        runCatching {
            // execute simple query to verify whether the db is connected
            // if the transaction throws exception, database is not connected
            transaction {
                this.connection.isClosed
            }
        }.isSuccess
}
