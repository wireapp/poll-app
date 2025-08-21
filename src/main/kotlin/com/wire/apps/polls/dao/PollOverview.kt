package com.wire.apps.polls.dao

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * Interactive message containing Poll participation progress bar and option to display results
 */
object PollOverview : Table("poll_overview") {
    val id: Column<String?> = varchar("id", UUID_LENGTH).nullable()

    /**
     * Reference to poll message
     */
    val pollId: Column<String> = varchar("poll_id", UUID_LENGTH) references Polls.id

    /**
     * Toggle to allow display of dynamic results
     */
    val resultsVisible: Column<Boolean> = bool("results_visible")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(pollId)
}
