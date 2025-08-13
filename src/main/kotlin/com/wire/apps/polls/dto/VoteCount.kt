package com.wire.apps.polls.dto

data class VoteCount(
    val votesCast: Int,
    val totalMembers: Int
) {
    companion object {
        fun initial() = VoteCount(0, 0)
    }

    fun everyoneVoted() = (votesCast == totalMembers)

    override fun toString(): String {
        return "Users voted: $votesCast, members of conversation: $totalMembers"
    }
}
