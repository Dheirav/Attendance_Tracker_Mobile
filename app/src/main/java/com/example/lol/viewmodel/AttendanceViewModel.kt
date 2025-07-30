package com.example.lol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lol.data.Attendance
import com.example.lol.data.AttendanceStatus
import com.example.lol.repository.AttendanceRepository
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

    fun markAttendance(subjectId: Int, date: String, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markAttendance(subjectId, date, status)
            // Update attended/total classes in Subject
            val subject = repository.getSubjectById(subjectId)
            if (subject != null) {
                val attended = subject.attendedClasses + if (status == AttendanceStatus.PRESENT) 1 else 0
                val total = subject.totalClasses + 1
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
            }
            loadAttendance(subjectId)
        }
    }
}
