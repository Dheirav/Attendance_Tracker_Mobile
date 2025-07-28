package com.example.lol.repository

import com.example.lol.data.Attendance
import com.example.lol.data.AttendanceDao
import com.example.lol.data.AttendanceStatus

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
    suspend fun markAttendance(subjectId: Int, date: String, status: AttendanceStatus) {
        val attendance = Attendance(subjectId = subjectId, date = date, status = status)
        attendanceDao.insertAttendance(attendance)
    }

    suspend fun getAttendanceHistory(subjectId: Int) = attendanceDao.getAttendanceForSubject(subjectId)

    suspend fun getAttendanceForDate(subjectId: Int, date: String) = attendanceDao.getAttendanceForSubjectOnDate(subjectId, date)

    suspend fun getAttendancePercentage(subjectId: Int): Double {
        val total = attendanceDao.getTotalMarked(subjectId)
        if (total == 0) return 0.0
        val present = attendanceDao.getCountByStatus(subjectId, AttendanceStatus.PRESENT)
        return (present.toDouble() / total) * 100
    }
}
