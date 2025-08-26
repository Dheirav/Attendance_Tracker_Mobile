package com.example.attendance_tracker.repository
import com.example.attendance_tracker.data.Attendance
import com.example.attendance_tracker.data.AttendanceDao
import com.example.attendance_tracker.data.AttendanceStatus
import com.example.attendance_tracker.data.SubjectRepository
import com.example.attendance_tracker.data.SubjectDao
import com.example.attendance_tracker.data.Subject
import kotlinx.coroutines.flow.first

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val subjectDao: SubjectDao,
    private val subjectRepository: SubjectRepository,
    private val timetableRepository: TimetableRepository // Injected shared instance
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

    suspend fun deleteAttendanceForSubjectOnDate(subjectId: Int, date: String) {
        attendanceDao.deleteAttendanceForSubjectOnDate(subjectId, date)
    }

    suspend fun updateAttendanceStatusForSubjectOnDate(subjectId: Int, date: String, status: AttendanceStatus) {
        val attendance = getAttendanceForDate(subjectId, date)
        if (attendance != null) {
            val subject = subjectDao.getSubjectById(subjectId)
            if (subject != null) {
                // Get day of week from date string
                val dayOfWeek = try {
                    java.time.LocalDate.parse(date).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
                } catch (e: Exception) { "" }
                // Get timetable entries for this day
                val timetableEntries = timetableRepository.getEntriesForDay(dayOfWeek).first()
                val entry = timetableEntries.firstOrNull { it.subject == subject.name }
                val slotCount = entry?.slotIds?.size ?: 1
                var attended = subject.attendedClasses
                // Adjust attended count based on status change
                if (attendance.status == AttendanceStatus.PRESENT && status == AttendanceStatus.ABSENT) {
                    attended = (attended - slotCount).coerceAtLeast(0)
                } else if (attendance.status == AttendanceStatus.ABSENT && status == AttendanceStatus.PRESENT) {
                    attended = attended + slotCount
                }
                subjectDao.updateSubject(subject.copy(attendedClasses = attended))
            }
            attendanceDao.updateAttendanceStatusForSubjectOnDate(subjectId, date, status)
        }
    }
}
