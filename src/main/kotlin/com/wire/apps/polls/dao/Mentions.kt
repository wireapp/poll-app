package com.wire.apps.polls.dao

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

/**
 * Table storing mentions of the users.
 */
object Mentions : IntIdTable("mentions") {
    /**
     * Id of the poll.
     */
    val pollId: Column<String> = varchar("poll_id", UUID_LENGTH) references Polls.id

    /**
     * Id of user that is mentioned.
     */
    val userId: Column<String> = varchar("user_id", UUID_LENGTH)

    /**
     * Domain of user that is mentioned.
     */
    val domain: Column<String> = varchar("domain", DOMAIN_LENGTH)

    /**
     * Where mention begins.
     */
    val offset: Column<Int> = integer("offset_shift")

    /**
     * Length of the mention.
     */
    val length: Column<Int> = integer("length")
}
