package com.yx.chatrobot.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yx.chatrobot.data.entity.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(conversation: Conversation): Long

    @Query("select * from conversation where user_id = :userId order by created_time desc")
    fun getConversationsByUser(userId: Int): Flow<List<Conversation>>

    @Query("select * from conversation where user_id = :userId order by created_time desc limit 1")
    suspend fun getLatestConversation(userId: Int): Conversation?

    @Query("select * from conversation where id = :id")
    suspend fun getConversationById(id: Int): Conversation?

    @Query("update conversation set title=:title where id=:id")
    suspend fun updateTitle(id: Int, title: String)

    @Query("delete from conversation where id=:id")
    suspend fun delete(id: Int)
}