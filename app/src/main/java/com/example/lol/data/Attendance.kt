package com.example.lol.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val date: String, // ISO format (yyyy-MM-dd)
    val status: AttendanceStatus
)

enum class AttendanceStatus {
    PRESENT, ABSENT
}
