package com.example.data

import kotlinx.coroutines.flow.Flow

class ReelNoteRepository(private val reelNoteDao: ReelNoteDao) {
    val allNotes: Flow<List<ReelNote>> = reelNoteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<ReelNote>> = reelNoteDao.searchNotes(query)

    suspend fun getNoteById(id: Long): ReelNote? = reelNoteDao.getNoteById(id)

    suspend fun insert(note: ReelNote): Long = reelNoteDao.insert(note)

    suspend fun update(note: ReelNote) = reelNoteDao.update(note)

    suspend fun delete(note: ReelNote) = reelNoteDao.delete(note)

    suspend fun deleteById(id: Long) = reelNoteDao.deleteById(id)
}
