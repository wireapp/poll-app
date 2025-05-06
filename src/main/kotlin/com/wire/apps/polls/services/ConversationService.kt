package com.wire.apps.polls.services

import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.service.WireApplicationManager

/**
 * Provides possibility to check the conversation details.
 */
class ConversationService {
    /**
     * Returns the number of members of conversation.
     */
    fun getNumberOfConversationMembers(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ): Int {
        return manager.getStoredConversationMembers(conversationId).size
    }
}
