package com.yx.chatrobot.data.repository

import com.yx.chatrobot.data.dao.MessageDao
import com.yx.chatrobot.data.entity.Message
import kotlinx.coroutines.flow.Flow

class OfflineMessageRepository(private val messageDao: MessageDao) : MessageRepository {
    override fun getMessagesStreamByUserId(userId: Int): Flow<List<Message>> =
        messageDao.getAllMessagesByUserId(userId)

    override fun getMessagesStreamBySessionId(sessionId: Int): Flow<List<Message>> =
        messageDao.getAllMessagesBySessionId(sessionId)

    override suspend fun insertMessage(message: Message): Long = messageDao.insert(message)

    override suspend fun getLastMessageBySessionId(sessionId: Int): Message? = messageDao.getLastMessageBySessionId(sessionId)

    override suspend fun updateContentAndStatus(id: Int, content: String, status: String) = messageDao.updateContentAndStatus(id, content, status)
    override suspend fun updateStatus(id: Int, status: String) = messageDao.updateStatus(id, status)

    override suspend fun getAllMessagesBySessionIdOnce(sessionId: Int): List<Message> = messageDao.getAllMessagesBySessionIdOnce(sessionId)
    override suspend fun deleteBySessionId(sessionId: Int) = messageDao.deleteBySessionId(sessionId)
}