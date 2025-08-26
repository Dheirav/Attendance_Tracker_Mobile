package com.example.attendance_tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance_tracker.data.Attendance
import com.example.attendance_tracker.data.AttendanceStatus
import com.example.attendance_tracker.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(val repository: AttendanceRepository) : ViewModel() {
    private val _attendanceHistory = MutableStateFlow<List<Attendance>>(emptyList())
    val attendanceHistory: StateFlow<List<Attendance>> = _attendanceHistory

    private val _attendancePercentage = MutableStateFlow(0.0)
    val attendancePercentage: StateFlow<Double> = _attendancePercentage

    fun loadAttendance(subjectId: Int) {
        viewModelScope.launch {
            _attendanceHistory.value = repository.getAttendanceHistory(subjectId)
            _attendancePercentage.value = repository.getAttendancePercentage(subjectId)
        }
    }

    fun markAttendance(subjectId: Int, selectedSlots: List<Int>, date: String, status: AttendanceStatus) {
        viewModelScope.launch {
            // Mark attendance for all slots, but only update attended/total once per subject per day
            selectedSlots.forEach { slotId ->
                repository.markAttendanceForSlot(subjectId, slotId, date, status)
            }
            val subject = repository.getSubjectById(subjectId)
            if (subject != null) {
                val slotCount = selectedSlots.size
                val attended = subject.attendedClasses + if (status == AttendanceStatus.PRESENT) slotCount else 0
                val total = subject.totalClasses + slotCount
                repository.updateSubjectAttendance(subjectId, attended, total)
            }
            loadAttendance(subjectId)
        }
    }

    fun updateManualAttendance(subjectId: Int, attended: Int, total: Int, note: String? = null, date: String? = null) {
        viewModelScope.launch {
            repository.updateSubjectAttendance(subjectId, attended, total)
            if (note != null && date != null) {
                repository.addManualHistory(subjectId, date, note)
                // Remove all previous attendance records for this subject
                repository.deleteAllAttendanceForSubject(subjectId)
                // Add new attendance records based on manual values
                val presentCount = attended
                val absentCount = total - attended
                val today = date
                repeat(presentCount) {
                    repository.markAttendanceForSlot(subjectId, -1, today, AttendanceStatus.PRESENT)
                }
                repeat(absentCount) {
                    repository.markAttendanceForSlot(subjectId, -1, today, AttendanceStatus.ABSENT)
                }
            }
            loadAttendance(subjectId)
        }
    }

    suspend fun getAttendanceStatusForSubjectOnDate(subjectId: Int, date: String): AttendanceStatus? {
        val attendance = repository.getAttendanceForDate(subjectId, date)
        return attendance?.status
    }

    suspend fun updateAttendanceStatusForSubjectOnDate(subjectId: Int, date: String, status: AttendanceStatus) {
        repository.updateAttendanceStatusForSubjectOnDate(subjectId, date, status)
    }

    suspend fun deleteAttendanceForSubjectOnDate(subjectId: Int, date: String) {
        repository.deleteAttendanceForSubjectOnDate(subjectId, date)
    }
}
