package com.wire.apps.polls.dto

data class PollVoteCountProgress(
    val totalVoteCount: Int,
    val totalMembers: Int
) {
    companion object {
        const val BLOCKS = 10
        const val PERCENTAGE_FACTOR = 100

        fun initial() = PollVoteCountProgress(0, 0)

        fun new(): String = initial().display()
    }

    /**
     * Percentage voting blocked until WPB-27239 is done
     */
    fun display(): String = "Total votes: $totalVoteCount"

    fun everyoneVoted() = (totalVoteCount == totalMembers)
}
