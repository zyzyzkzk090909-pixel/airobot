package com.yx.chatrobot.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation")
data class Conversation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "新对话",
    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis() / 1000,
    @ColumnInfo(name = "user_id")
    val userId: Int = 0
)