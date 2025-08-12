package com.example.attendance_tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "Core", "Elective", "Lab"
    val threshold: Int = 75,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0
)
