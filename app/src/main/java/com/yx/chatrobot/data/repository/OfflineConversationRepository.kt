package com.yx.chatrobot.data.repository

import com.yx.chatrobot.data.dao.ConversationDao
import com.yx.chatrobot.data.entity.Conversation
import kotlinx.coroutines.flow.Flow

class OfflineConversationRepository(private val dao: ConversationDao) : ConversationRepository {
    override fun getConversationsByUser(userId: Int): Flow<List<Conversation>> =
        dao.getConversationsByUser(userId)

    override suspend fun getLatestConversation(userId: Int): Conversation? = dao.getLatestConversation(userId)

    override suspend fun insert(conversation: Conversation): Int = dao.insert(conversation).toInt()

    override suspend fun getById(id: Int): Conversation? = dao.getConversationById(id)

    override suspend fun updateTitle(id: Int, title: String) = dao.updateTitle(id, title)
    override suspend fun delete(id: Int) = dao.delete(id)
}