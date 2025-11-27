package com.yx.chatrobot.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yx.chatrobot.data.entity.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: Message): Long

    @Query("SELECT * from message WHERE id = :id")
    fun getMessage(id: Int): Flow<Message>

    @Query("Select * from  message where user_id = :userId order by time asc")
    fun getAllMessagesByUserId(userId: Int): Flow<List<Message>>

    @Query("Select * from  message where session_id = :sessionId order by time asc")
    fun getAllMessagesBySessionId(sessionId: Int): Flow<List<Message>>

    @Query("Select * from  message where session_id = :sessionId order by time desc limit 1")
    suspend fun getLastMessageBySessionId(sessionId: Int): Message?

    @Query("update message set content=:content, status=:status where id=:id")
    suspend fun updateContentAndStatus(id: Int, content: String, status: String)

    @Query("update message set status=:status where id=:id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("Select * from  message where session_id = :sessionId order by time asc")
    suspend fun getAllMessagesBySessionIdOnce(sessionId: Int): List<Message>

    @Query("delete from message where session_id=:sessionId")
    suspend fun deleteBySessionId(sessionId: Int)

}