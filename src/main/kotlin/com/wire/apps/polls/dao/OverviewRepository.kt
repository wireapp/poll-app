package com.wire.apps.polls.dao

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class OverviewRepository {
    suspend fun getPollMessage(pollOverviewMessageId: String) =
        newSuspendedTransaction {
            PollOverview.select { PollOverview.id eq pollOverviewMessageId }
                .singleOrNull()?.get(PollOverview.pollId)
        }

    suspend fun showResults(pollId: String) =
        newSuspendedTransaction {
            PollOverview.update({
                PollOverview.pollId eq pollId
            }) { it[this.resultsVisible] = true }
        }

    suspend fun areResultsVisible(pollId: String): Boolean {
        return newSuspendedTransaction {
            PollOverview
                .select { PollOverview.pollId eq pollId }
                .single()[PollOverview.resultsVisible]
        }
    }

    suspend fun setParticipationId(
        pollId: String,
        participationMessageId: String
    ) = newSuspendedTransaction {
        PollOverview.update({
            PollOverview.pollId eq pollId
        }) { it[this.id] = participationMessageId }
    }

    suspend fun getParticipationId(pollId: String) =
        newSuspendedTransaction {
            PollOverview
                .select { PollOverview.pollId eq pollId }
                .singleOrNull()?.get(PollOverview.id)
        }
}
