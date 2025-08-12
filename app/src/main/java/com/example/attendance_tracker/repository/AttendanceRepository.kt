package com.example.attendance_tracker.repository
import com.example.attendance_tracker.data.Attendance
import com.example.attendance_tracker.data.AttendanceDao
import com.example.attendance_tracker.data.AttendanceStatus
import com.example.attendance_tracker.data.SubjectRepository
import com.example.attendance_tracker.data.SubjectDao
import com.example.attendance_tracker.data.Subject

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val subjectDao: SubjectDao,
    private val subjectRepository: SubjectRepository // Injected shared instance
) {
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
            // Notify shared SubjectRepository to refresh subjects
            subjectRepository.refreshSubjects()
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
