package com.example.attendance_tracker.data

import androidx.room.*

@Dao
interface AttendanceDao {
    @Query("DELETE FROM attendance WHERE subjectId = :subjectId")
    suspend fun deleteAllForSubject(subjectId: Int)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId AND slotId = :slotId ORDER BY date DESC")
    suspend fun getAttendanceForSlot(subjectId: Int, slotId: Int): List<Attendance>

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

    @Query("DELETE FROM attendance WHERE subjectId = :subjectId AND date = :date")
    suspend fun deleteAttendanceForSubjectOnDate(subjectId: Int, date: String)

    @Query("UPDATE attendance SET status = :status WHERE subjectId = :subjectId AND date = :date")
    suspend fun updateAttendanceStatusForSubjectOnDate(subjectId: Int, date: String, status: AttendanceStatus)
}
