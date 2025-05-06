package com.wire.apps.polls.dao

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * Poll options.
 */
object PollOptions : Table("poll_option") {
    /**
     * Id of the poll this option is for. UUID.
     */
    val pollId: Column<String> = varchar("poll_id", UUID_LENGTH) references Polls.id

    /**
     * Option order or option id.
     */
    val optionOrder: Column<Int> = integer("option_order")

    /**
     * Option content, the text inside the button/choice.
     */
    private const val OPTION_LENGTH = 255
    val optionContent: Column<String> = varchar("option_content", OPTION_LENGTH)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(pollId, optionOrder)
}
