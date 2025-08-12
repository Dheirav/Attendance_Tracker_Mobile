package com.example.attendance_tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance_tracker.data.AppDatabase
import com.example.attendance_tracker.data.CommonSlotEntity
import com.example.attendance_tracker.repository.CommonSlotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommonSlotViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).commonSlotDao()
    private val repository = CommonSlotRepository(dao)

    val slots: StateFlow<List<CommonSlotEntity>> =
        repository.getAllSlots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Remove default slot insertion from init. Call insertDefaultSlotsIfEmpty() from MainActivity after DB creation.

    fun insertSlot(slot: CommonSlotEntity) = viewModelScope.launch { repository.insertSlot(slot) }
    fun updateSlot(slot: CommonSlotEntity) = viewModelScope.launch { repository.updateSlot(slot) }
    fun deleteSlot(slot: CommonSlotEntity) = viewModelScope.launch { repository.deleteSlot(slot) }

    fun insertDefaultSlotsIfEmpty() = viewModelScope.launch {
        if (dao.getSlotCount() == 0) {
            val defaultSlots = listOf(
                CommonSlotEntity(label = "Period 1", startTime = "08:30 AM", endTime = "09:20 AM"),
                CommonSlotEntity(label = "Period 2", startTime = "09:25 AM", endTime = "10:15 AM"),
                CommonSlotEntity(label = "Period 3", startTime = "10:30 AM", endTime = "11:20 AM"),
                CommonSlotEntity(label = "Period 4", startTime = "11:25 AM", endTime = "12:15 PM"),
                CommonSlotEntity(label = "Period 5", startTime = "01:10 PM", endTime = "02:00 PM"),
                CommonSlotEntity(label = "Period 6", startTime = "02:05 PM", endTime = "02:55 PM"),
                CommonSlotEntity(label = "Period 7", startTime = "03:00 PM", endTime = "03:50 PM"),
                CommonSlotEntity(label = "Period 8", startTime = "03:55 PM", endTime = "04:45 PM"),
                CommonSlotEntity(label = "Period 9", startTime = "04:50 PM", endTime = "05:40 PM")
            )
            defaultSlots.forEach { repository.insertSlot(it) }
        }
    }
}