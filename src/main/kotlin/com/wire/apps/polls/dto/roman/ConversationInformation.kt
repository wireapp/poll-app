package com.wire.apps.polls.dto.roman

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// here we basically don't care about anything (yet) as we want just number of members

/**
 * Conversation Roman endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ConversationInformation(
    // conversation id
    val id: String,
    val name: String?,
    val creator: String?,
    val members: List<Member>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Member(
    val id: String?,
    val status: Int?,
    val service: Service?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Service(
    val id: String?,
    val provider: String?
)

/*
{
  "id": "string",
  "name": "string",
  "creator": "string",
  "members": [
    {
      "id": "string",
      "status": 0,
      "service": {
        "id": "string",
        "provider": "string"
      }
    }
  ]
}
 */
