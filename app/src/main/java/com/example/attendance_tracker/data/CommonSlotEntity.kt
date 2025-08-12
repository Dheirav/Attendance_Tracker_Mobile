package com.example.attendance_tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "common_slots")
data class CommonSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val startTime: String,
    val endTime: String
)