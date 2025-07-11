package com.wire.apps.polls.dao

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * Table storing votes of the users.
 */
object Votes : Table("votes") {
    /**
     * Id of the poll.
     */
    val pollId: Column<String> = varchar("poll_id", UUID_LENGTH)

    /**
     * Id of the option.
     */
    val pollOption: Column<Int> = integer("poll_option")

    /**
     * User who voted for this option.
     */
    val userId: Column<String> = varchar("user_id", UUID_LENGTH)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(pollId, userId)
}
