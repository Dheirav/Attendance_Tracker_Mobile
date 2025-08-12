package com.example.attendance_tracker.repository

import com.example.attendance_tracker.data.TimetableDao
import com.example.attendance_tracker.data.TimetableEntry
import kotlinx.coroutines.flow.Flow

class TimetableRepository(private val dao: TimetableDao) {
    fun getEntriesForDay(day: String): Flow<List<TimetableEntry>> = dao.getEntriesForDay(day)
    suspend fun insert(entry: TimetableEntry) = dao.insert(entry)
    suspend fun delete(entry: TimetableEntry) = dao.delete(entry)
}
