package com.wire.apps.polls.dto.common

import com.wire.sdk.model.QualifiedId
import com.wire.sdk.model.WireMessage
import java.util.UUID

data class Mention(
    val userId: String,
    val userDomain: String,
    val offset: Int,
    val length: Int
) {
    companion object {
        private fun fromWire(mention: WireMessage.Mention): Mention =
            Mention(
                userId = mention.userId.id.toString(),
                userDomain = mention.userId.domain,
                offset = mention.offset,
                length = mention.length
            )

        fun fromWireList(mentions: List<WireMessage.Mention>): List<Mention> =
            mentions.map { fromWire(it) }
    }
}

fun Mention.toWireMention(): WireMessage.Mention =
    WireMessage.Mention(
        userId = QualifiedId(
            UUID.fromString(this.userId),
            this.userDomain
        ),
        offset = this.offset,
        length = this.length
    )
