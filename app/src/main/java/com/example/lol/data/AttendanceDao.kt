package com.example.lol.data

import androidx.room.*

@Dao
interface AttendanceDao {
    @Query("DELETE FROM attendance WHERE subjectId = :subjectId")
    suspend fun deleteAllForSubject(subjectId: Int)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    // Insert manual update history
    @Insert
    suspend fun insertManualHistory(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId ORDER BY date DESC")
    suspend fun getAttendanceForSubject(subjectId: Int): List<Attendance>

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId AND date = :date LIMIT 1")
    suspend fun getAttendanceForSubjectOnDate(subjectId: Int, date: String): Attendance?

    @Query("SELECT COUNT(*) FROM attendance WHERE subjectId = :subjectId AND status = :status")
    suspend fun getCountByStatus(subjectId: Int, status: AttendanceStatus): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE subjectId = :subjectId")
    suspend fun getTotalMarked(subjectId: Int): Int
}
