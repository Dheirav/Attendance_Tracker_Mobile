package com.example.lol.repository

import com.example.lol.data.TimetableDao
import com.example.lol.data.TimetableEntry
import kotlinx.coroutines.flow.Flow

class TimetableRepository(private val dao: TimetableDao) {
    fun getEntriesForDay(day: String): Flow<List<TimetableEntry>> = dao.getEntriesForDay(day)
    suspend fun insert(entry: TimetableEntry) = dao.insert(entry)
    suspend fun delete(entry: TimetableEntry) = dao.delete(entry)
}
