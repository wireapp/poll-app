package com.wire.bots.polls.dao

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

/**
 * Polls table definition.
 */
object Polls : Table("polls") {
    /**
     * Id of the poll. UUID.
     */
    val id: Column<String> = varchar("id", UUID_LENGTH).uniqueIndex()

    /**
     * Id of the user who created this poll. UUID.
     */
    val ownerId: Column<String> = varchar("owner_id", UUID_LENGTH)

    /**
     * Id of the conversation where poll was created. UUID.
     */
    val conversationId: Column<String> = varchar("conversation_id", UUID_LENGTH)

    /**
     * Domain of conversation.
     */
    val domain: Column<String> = varchar("domain", DOMAIN_LENGTH)

    /**
     * Determines whether is the pool active and whether new votes should be accepted.
     */
    val isActive: Column<Boolean> = bool("is_active")

    /**
     * Contains body of the poll - the question.
     */
    val body: Column<String> = text("body", collate = null)

    /**
     * Timestamp when was this poll created.
     */
    val created: Column<LocalDateTime> = datetime("time_stamp")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(id)
}
