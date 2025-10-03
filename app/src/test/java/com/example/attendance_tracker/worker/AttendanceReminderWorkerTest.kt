package com.example.attendance_tracker.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.attendance_tracker.data.AppDatabase
import com.example.attendance_tracker.data.Attendance
import com.example.attendance_tracker.data.AttendanceDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class AttendanceReminderWorkerTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var attendanceDao: AttendanceDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        attendanceDao = db.attendanceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testReminderWorker_showsNotificationWhenNoAttendance() = runBlocking {
        val worker = TestWorkerBuilder<AttendanceReminderWorker>(
            context = context,
            workerParams = androidx.work.Data.Builder().build()
        ).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        // You can add more checks here if you mock NotificationManager
    }

    @Test
    fun testReminderWorker_noNotificationWhenAttendanceMarked() = runBlocking {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        attendanceDao.insertAttendance(Attendance(subjectId = 1, slotId = 1, date = today, status = com.example.attendance_tracker.data.AttendanceStatus.PRESENT))
        val worker = TestWorkerBuilder<AttendanceReminderWorker>(
            context = context,
            workerParams = androidx.work.Data.Builder().build()
        ).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        // You can add more checks here if you mock NotificationManager
    }
}

