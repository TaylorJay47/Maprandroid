package com.maprandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val romPath: String,
    val mapLayout: String,
    val skillAssumptions: String = "Medium",
    val configProfileJson: String = "{}",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
