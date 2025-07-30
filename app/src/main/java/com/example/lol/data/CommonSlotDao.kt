package com.example.lol.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommonSlotDao {
    @Query("SELECT * FROM common_slots")
    fun getAllSlots(): Flow<List<CommonSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: CommonSlotEntity)

    @Update
    suspend fun updateSlot(slot: CommonSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: CommonSlotEntity)

    @Query("SELECT COUNT(*) FROM common_slots")
    suspend fun getSlotCount(): Int
}