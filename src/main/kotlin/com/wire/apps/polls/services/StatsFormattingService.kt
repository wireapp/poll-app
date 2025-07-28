package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.common.Text
import mu.KLogging
import pw.forst.katlib.newLine
import pw.forst.katlib.whenNull
import kotlin.math.min

class StatsFormattingService(
    private val repository: PollRepository
) {
    private companion object : KLogging() {
        const val TITLE_PREFIX = "**Results** for poll *\""

        /**
         * Maximum number of trailing vote slots to be displayed, considered the most voted option.
         */
        const val MAX_VOTE_PLACEHOLDER_COUNT = 2
    }

    /**
     * Prepares message with statistics about the poll to the proxy.
     * When conversationMembers is null, stats are formatted according to the max votes per option.
     */
    suspend fun formatStats(
        pollId: String,
        conversationMembers: Int
    ): Text? {
        val pollQuestion = repository.getPollQuestion(pollId).whenNull {
            logger.warn { "No poll $pollId exists." }
        } ?: return null

        val stats = repository.stats(pollId)
        return if (stats.isEmpty()) {
            logger.info { "There are no data for given poll $pollId." }
            null
        } else {
            val title = prepareTitle(pollQuestion.data)
            val options = formatVotes(stats, conversationMembers)
            Text(
                data = "$title$newLine$options",
                mentions = pollQuestion.mentions.map {
                    it.copy(
                        offset = it.offset + TITLE_PREFIX.length
                    )
                }
            )
        }
    }

    /**
     * Formats the vote results using the most voted option to determine the output size.
     * Will add [MAX_VOTE_PLACEHOLDER_COUNT] number of trailing placeholders to
     * until it reaches [conversationMembers].
     *
     * Examples:
     * With [MAX_VOTE_PLACEHOLDER_COUNT] = 2 and [conversationMembers] >= 5:
     * - ⬛⬜⬜⬜⬜ A (1)
     * - ⬛⬛⬛⬜⬜ B (3)
     * - ⬛⬛⬜⬜⬜ C (2)
     *
     * With [MAX_VOTE_PLACEHOLDER_COUNT] = 2 and 4 [conversationMembers] = 4:
     * - ⬜⬜⬜⬜ A (0)
     * - ⬛⬛⬛⬜ B (3)
     * - ⬛⬜⬜⬜ C (1)
     *
     * With [MAX_VOTE_PLACEHOLDER_COUNT] = 2 and 3 [conversationMembers] = 3:
     * - ⬛⬛⬛ A (3)
     * - ⬜⬜⬜ B (1)
     */
    private fun formatVotes(
        stats: Map<Pair<Int, String>, Int>,
        conversationMembers: Int
    ): String {
        // we can use assert as the result size is checked
        val mostPopularOptionVoteCount =
            requireNotNull(stats.values.maxOrNull()) { "There were no stats!" }

        val maximumSize = min(
            conversationMembers,
            mostPopularOptionVoteCount + MAX_VOTE_PLACEHOLDER_COUNT
        )

        return stats
            .map { (option, votingUsers) ->
                VotingOption(
                    style = if (votingUsers ==
                        mostPopularOptionVoteCount
                    ) {
                        "**"
                    } else {
                        "*"
                    },
                    option = option.second,
                    votingUsers = votingUsers
                )
            }.let { votes ->
                votes.joinToString(newLine) { it.toString(maximumSize) }
            }
    }

    private fun prepareTitle(body: String) = "$TITLE_PREFIX${body}\"*"
}

/**
 * Class used for formatting voting objects.
 */
private data class VotingOption(
    val style: String,
    val option: String,
    val votingUsers: Int
) {
    private companion object {
        const val NOT_VOTE = "⚪"
        const val VOTE = "🟢"
    }

    fun toString(max: Int): String {
        val missingVotes = (0 until max - votingUsers).joinToString("") { NOT_VOTE }
        val votes = (0 until votingUsers).joinToString("") { VOTE }
        return "$votes$missingVotes $style$option$style ($votingUsers)"
    }
}
