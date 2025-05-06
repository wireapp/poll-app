package com.wire.bots.polls.dao

import com.wire.bots.polls.dto.PollDto
import com.wire.bots.polls.dto.Question
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import mu.KLogging
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import pw.forst.exposed.insertOrUpdate
import pw.forst.katlib.mapToSet

/**
 * Simple repository for handling database transactions on one place.
 */
class PollRepository {
    private companion object : KLogging()

    /**
     * Saves given poll to database and returns its id (same as the [pollId] parameter,
     * but this design supports fluent style in the services.
     */
    suspend fun savePoll(
        poll: PollDto,
        pollId: String,
        userId: String,
        userDomain: String,
        conversationId: String
    ) = newSuspendedTransaction {
        Polls.insert {
            it[this.id] = pollId
            it[this.ownerId] = userId
            it[this.domain] = userDomain
            it[this.isActive] = true
            it[this.conversationId] = conversationId
            it[this.body] = poll.question.body
        }

        Mentions.batchInsert(poll.question.mentions) {
            this[Mentions.pollId] = pollId
            this[Mentions.userId] = it.userId?.id.toString()
            this[Mentions.domain] = it.userId?.domain.toString()
            this[Mentions.offset] = it.offset
            this[Mentions.length] = it.length
        }

        PollOptions.batchInsert(
            poll.options
        ) {
            this[PollOptions.pollId] = pollId
            this[PollOptions.optionOrder] = it.id.toInt()
            this[PollOptions.optionContent] = it.text
        }
        pollId
    }

    /**
     * Returns question for given poll Id. If the poll does not exist, null is returned.
     */
    suspend fun getPollQuestion(pollId: String) =
        newSuspendedTransaction {
            (Polls leftJoin Mentions)
                .select { Polls.id eq pollId }
                .groupBy(
                    { it[Polls.body] },
                    {
                        if (it.getOrNull(Mentions.userId) != null) {
                            WireMessage.Text.Mention(
                                userId = null,
                                offset = it[Mentions.offset],
                                length = it[Mentions.length]
                            )
                        } else {
                            null
                        }
                    }
                ).map { (pollBody, mentions) ->
                    Question(body = pollBody, mentions = mentions.filterNotNull())
                }.singleOrNull()
        }

    /**
     * Register new vote to the poll. If the poll with provided pollId does not exist,
     * database contains foreign key to an option and poll so the SQL exception is thrown.
     */
    suspend fun vote(pollAction: WireMessage.ButtonAction) =
        newSuspendedTransaction {
            Votes.insertOrUpdate(Votes.pollId, Votes.userId) {
                it[pollId] = pollAction.referencedMessageId
                it[pollOption] = pollAction.buttonId.toInt()
                it[userId] = pollAction.sender.id.toString()
            }
        }

    /**
     * Retrieves stats for given pollId.
     *
     * Offset/option/button content as keys and count of the votes as values.
     */
    suspend fun stats(pollId: String) =
        newSuspendedTransaction {
            PollOptions
                .join(
                    Votes,
                    JoinType.LEFT,
                    additionalConstraint = {
                        (Votes.pollId eq PollOptions.pollId) and
                            (Votes.pollOption eq PollOptions.optionOrder)
                    }
                ).slice(PollOptions.optionOrder, PollOptions.optionContent, Votes.userId)
                .select { PollOptions.pollId eq pollId }
                // left join so userId can be null
                .groupBy({
                    it[PollOptions.optionOrder] to it[PollOptions.optionContent]
                }, { it.getOrNull(Votes.userId) })
                .mapValues { (_, votingUsers) -> votingUsers.count { !it.isNullOrBlank() } }
        }

    /**
     * Returns set of user ids that voted in the poll with given pollId.
     */
    suspend fun votingUsers(pollId: String) =
        newSuspendedTransaction {
            Votes
                .slice(Votes.userId)
                .select { Votes.pollId eq pollId }
                .mapToSet { it[Votes.userId] }
        }

    /**
     * Returns set of user ids that voted in the poll with given pollId.
     */
    suspend fun getLatestForConversation(conversationId: QualifiedId) =
        newSuspendedTransaction {
            Polls
                .slice(Polls.id)
                // it must be for single bot
                .select { Polls.conversationId eq conversationId.id.toString() }
                // such as latest is on top
                .orderBy(Polls.created to SortOrder.DESC)
                // select just one
                .limit(1)
                .singleOrNull()
                ?.get(Polls.id)
        }
}
