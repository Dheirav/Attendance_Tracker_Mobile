package com.example.lol.repository

import com.example.lol.data.CommonSlotDao
import com.example.lol.data.CommonSlotEntity
import kotlinx.coroutines.flow.Flow

class CommonSlotRepository(private val dao: CommonSlotDao) {
    fun getAllSlots(): Flow<List<CommonSlotEntity>> = dao.getAllSlots()
    suspend fun insertSlot(slot: CommonSlotEntity) = dao.insertSlot(slot)
    suspend fun updateSlot(slot: CommonSlotEntity) = dao.updateSlot(slot)
    suspend fun deleteSlot(slot: CommonSlotEntity) = dao.deleteSlot(slot)
}