package com.example.attendance_tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM TimetableEntry WHERE dayOfWeek = :day")
    fun getEntriesForDay(day: String): Flow<List<TimetableEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimetableEntry)

    @Delete
    suspend fun delete(entry: TimetableEntry)
}
