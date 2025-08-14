package com.wire.apps.polls.dao

import com.wire.apps.polls.setup.conf.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Configure and check database
 */
object DatabaseSetup {
    fun connect(dbConfiguration: DatabaseConfiguration) =
        Database.connect(
            url = dbConfiguration.url,
            user = dbConfiguration.userName,
            password = dbConfiguration.password,
            driver = "org.postgresql.Driver"
        )

    /**
     * Ensure DB connection is alive before performing operations
     */
    fun isConnected() =
        runCatching {
            transaction {
                this.connection.isClosed
            }
        }.isSuccess
}
