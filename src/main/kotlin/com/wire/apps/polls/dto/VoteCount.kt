package com.wire.apps.polls.dto

import com.wire.apps.polls.services.VoteDisplay
import kotlin.math.roundToInt

data class VoteCount(
    val votesCast: Int,
    val totalMembers: Int
) {
    companion object {
        const val BLOCKS = 10
        const val PERCENTAGE_FACTOR = 100

        fun initial() = VoteCount(0, 0)

        fun new(): String {
            return initial().display()
        }
    }

    fun display(): String {
        val percent = if (totalMembers > 0) votesCast.toDouble() / totalMembers else 0.0
        val voteDisplay = VoteDisplay((percent * BLOCKS).roundToInt(), BLOCKS)
        val percentDisplay = (percent * PERCENTAGE_FACTOR).roundToInt()

        return "$voteDisplay $percentDisplay%"
    }

    fun everyoneVoted() = (votesCast == totalMembers)

    override fun toString(): String {
        return "Users voted: $votesCast, members of conversation: $totalMembers"
    }
}
