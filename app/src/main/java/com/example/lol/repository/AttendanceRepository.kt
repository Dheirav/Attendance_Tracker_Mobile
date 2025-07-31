package com.example.lol.repository
import com.example.lol.data.Attendance
import com.example.lol.data.AttendanceDao
import com.example.lol.data.AttendanceStatus
import com.example.lol.data.SubjectDao
import com.example.lol.data.Subject

class AttendanceRepository(private val attendanceDao: AttendanceDao, private val subjectDao: SubjectDao) {
    suspend fun markAttendanceForSlot(subjectId: Int, slotId: Int, date: String, status: AttendanceStatus) {
        val attendance = Attendance(subjectId = subjectId, slotId = slotId, date = date, status = status)
        attendanceDao.insertAttendance(attendance)
    }

    suspend fun deleteAllAttendanceForSubject(subjectId: Int) {
        attendanceDao.deleteAllForSubject(subjectId)
    }

    suspend fun getSubjectById(subjectId: Int): Subject? {
        return subjectDao.getSubjectById(subjectId)
    }
    
    suspend fun addManualHistory(subjectId: Int, date: String, note: String) {
        val manual = Attendance(subjectId = subjectId, slotId = -1, date = date, status = AttendanceStatus.PRESENT, note = note)
        attendanceDao.insertManualHistory(manual)
    }

    suspend fun updateSubjectAttendance(subjectId: Int, attended: Int, total: Int) {
        val subject = subjectDao.getSubjectById(subjectId)
        if (subject != null) {
            val updated = subject.copy(attendedClasses = attended, totalClasses = total)
            subjectDao.updateSubject(updated)
        }
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
