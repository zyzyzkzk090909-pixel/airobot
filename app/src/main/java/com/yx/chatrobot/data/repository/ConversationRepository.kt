package com.yx.chatrobot.data.repository

import com.yx.chatrobot.data.entity.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getConversationsByUser(userId: Int): Flow<List<Conversation>>
    suspend fun getLatestConversation(userId: Int): Conversation?
    suspend fun insert(conversation: Conversation): Int
    suspend fun getById(id: Int): Conversation?
    suspend fun updateTitle(id: Int, title: String)
    suspend fun delete(id: Int)
}