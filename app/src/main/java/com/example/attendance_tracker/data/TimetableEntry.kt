package com.example.attendance_tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // e.g. "Monday"
    val subject: String,
    val startTime: String, // e.g. "09:00"
    val endTime: String,   // e.g. "10:00"
    val slotIds: List<Int> 
)
