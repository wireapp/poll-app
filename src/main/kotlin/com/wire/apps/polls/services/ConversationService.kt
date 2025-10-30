package com.wire.apps.polls.services

import com.wire.sdk.model.QualifiedId
import com.wire.sdk.service.WireApplicationManager

/**
 * Provides possibility to check the conversation details.
 */
class ConversationService {
    /**
     * Used to determine if voting is complete and to format poll stats accordingly.
     */
    fun getNumberOfConversationMembers(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ): Int = manager.getStoredConversationMembers(conversationId).size
}
