package com.wire.apps.polls.utils

import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.dto.common.Mention
import com.wire.sdk.model.QualifiedId
import java.util.UUID

internal object Stub {
    fun id() = QualifiedId(UUID.randomUUID(), "test.domain.link")

    fun userInput(
        text: String,
        mentions: List<Mention> = emptyList()
    ) = UsersInput(
        sender = id(),
        conversationId = id(),
        text = text,
        mentions = mentions
    )

    fun mention(
        text: String,
        userName: String,
        qualifiedId: QualifiedId? = null
    ) = Mention(
        userId = qualifiedId?.id?.toString() ?: id().id.toString(),
        userDomain = qualifiedId?.domain ?: id().domain,
        offset = text.indexOf(userName),
        length = userName.length
    )
}
