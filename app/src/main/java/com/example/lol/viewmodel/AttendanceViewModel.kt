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
            loadAttendance(subjectId)
        }
    }
}
