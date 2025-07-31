package com.example.lol.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val slotId: Int, // ID of the slot attended
    val date: String, // ISO format (yyyy-MM-dd)
    val status: AttendanceStatus,
    val note: String? = null // for manual update history
)

enum class AttendanceStatus {
    PRESENT, ABSENT
}
