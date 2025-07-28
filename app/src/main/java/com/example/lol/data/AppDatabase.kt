package com.example.lol.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Subject::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
}
