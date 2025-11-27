package com.yx.chatrobot.data.repository

import com.yx.chatrobot.data.entity.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {

    fun getMessagesStreamByUserId(userId: Int): Flow<List<Message>>

    fun getMessagesStreamBySessionId(sessionId: Int): Flow<List<Message>>

    suspend fun insertMessage(message: Message): Long

    suspend fun getLastMessageBySessionId(sessionId: Int): Message?

    suspend fun updateContentAndStatus(id: Int, content: String, status: String)
    suspend fun updateStatus(id: Int, status: String)

    suspend fun getAllMessagesBySessionIdOnce(sessionId: Int): List<Message>
    suspend fun deleteBySessionId(sessionId: Int)


}