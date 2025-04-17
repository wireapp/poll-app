package com.wire.bots.polls.dto.roman

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.wire.bots.polls.dto.common.Mention

/**
 * Message received by the bot from the proxy.
 *
 * TODO generate that from the swagger automatically
 */
@JsonIgnoreProperties(ignoreUnknown = true) // to continue working even though the JSON changed
data class Message(
    /**
     * ID of the bot = the bot should accept the message only when the ID matches.
     */
    val botId: String,
    /**
     * User who sent a message.
     */
    val userId: String?,
    /**
     * Id of the conversation.
     */
    val conversationId: String?,
    /**
     * Type of the message.
     */
    val type: String,
    /**
     * Message ID.
     */
    val messageId: String?,
    /**
     * Token that should be used for the reply.
     */
    val token: String?,
    /**
     * Text of the message.
     */
    val text: Text?,
    /**
     * Id of the quoted message, when the user replies on something, this is id of something.
     */
    val refMessageId: String?,
    /**
     * When this and [refMessageId] is filled, the user liked the message with id [refMessageId].
     */
    val reaction: String?,
    /**
     * Image in the message.
     */
    val image: String?,
    /**
     * Username who initiated conversation.
     */
    val handle: String?,
    /**
     * Language of the user.
     */
    val locale: String?,
    /**
     * Poll object.
     */
    val poll: PollObjectMessage?,
    /**
     * Type of the file
     */
    val mimeType: String?
) {
    data class Text(
        val data: String,
        val mentions: List<Mention>?
    ) {
        override fun toString(): String = "Text(mentions=$mentions)"
    }

    /**
     * Poll representation for the proxy.
     */
    data class PollObjectMessage(
        /**
         * Id of the poll.
         */
        val id: String,
        /**
         * Body of the poll - the question.
         */
        val body: String?,
        /**
         * Ordered list of buttons. Position in the list is the ID / offset.
         */
        val buttons: List<String>?,
        /**
         * Id of the button when it was clicked on.
         */
        val offset: Int?
    ) {
        override fun toString(): String =
            "PollObjectMessage(id='$id', buttons=$buttons, offset=$offset)"
    }

    /**
     * Avoid printing out the token by mistake if object is printed.
     */
    override fun toString(): String =
        "Message(botId='$botId', userId=$userId, conversationId=$conversationId, type='$type', " +
            "messageId=$messageId, text=$text, refMessageId=$refMessageId, reaction=$reaction, " +
            "image=$image, handle=$handle, locale=$locale, poll=$poll, mimeType=$mimeType)"
}

/* JSON from the swagger
{
  "botId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "string",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "handle": "string",
  "locale": "string",
  "token": "string",
  "messageId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "refMessageId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "conversation": "string",
  "text": {
    "data": "string",
    "mentions": [
      {
        "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "offset": 0,
        "length": 0
      }
    ]
  },
  "attachment": {
    "data": "string",
    "name": "string",
    "mimeType": "string",
    "size": 0,
    "duration": 0,
    "levels": [
      "string"
    ],
    "height": 0,
    "width": 0,
    "meta": {
      "assetId": "string",
      "assetToken": "string",
      "sha256": "string",
      "otrKey": "string"
    }
  },
  "poll": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "type": "string",
    "buttons": [
      "string"
    ],
    "offset": 0,
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  },
  "call": {
    "version": "string",
    "type": "string",
    "resp": true,
    "sessid": "string",
    "props": {
      "additionalProp1": "string",
      "additionalProp2": "string",
      "additionalProp3": "string"
    }
  },
  "emoji": "string"
}

 */
