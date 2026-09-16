package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reel_notes")
data class ReelNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reelUrl: String,
    val shortCode: String = "",
    val title: String,
    val useCase: String, // "What will I use this for?" e.g., "Sunday dinner recipe"
    val notes: String = "", // Detailed notes, steps, ingredients, etc.
    val category: String = "Ideas", // Recipe, Fitness, Travel, Creative, Shopping, Tutorial, Life Hack, Ideas
    val actionStatus: String = "TO_TRY", // TO_TRY, TRIED, SAVED
    val isFavorite: Boolean = false,
    val rawSharedText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
