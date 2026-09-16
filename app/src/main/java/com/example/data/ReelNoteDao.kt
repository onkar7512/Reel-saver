package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelNoteDao {
    @Query("SELECT * FROM reel_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<ReelNote>>

    @Query("SELECT * FROM reel_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): ReelNote?

    @Query("""
        SELECT * FROM reel_notes 
        WHERE title LIKE '%' || :query || '%' 
           OR useCase LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<ReelNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: ReelNote): Long

    @Update
    suspend fun update(note: ReelNote)

    @Delete
    suspend fun delete(note: ReelNote)

    @Query("DELETE FROM reel_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM reel_notes")
    suspend fun getCount(): Int
}
