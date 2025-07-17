package com.wire.apps.polls.dto

data class PollParticipation(
    val votesCast: Int,
    val totalMembers: Int
) {
    companion object {
        fun initial() = PollParticipation(0, 0)
    }

    fun everyoneVoted() = (votesCast == totalMembers)

    override fun toString(): String {
        return "Users voted: $votesCast, members of conversation: $totalMembers"
    }
}
