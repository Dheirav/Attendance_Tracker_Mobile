package com.example.attendance_tracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.attendance_tracker.R
import com.example.attendance_tracker.data.AppDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val attendanceDao = db.attendanceDao()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val attendanceCount = attendanceDao.getAttendanceCountForDate(today)
        if (attendanceCount == 0) {
            showNotification()
        }
        return Result.success()
    }

    private fun showNotification() {
        val channelId = "attendance_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Attendance Reminder"
            val descriptionText = "Reminds you to mark your attendance."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon for notification
            .setContentTitle("Attendance Reminder")
            .setContentText("You haven't marked your attendance today. Tap to mark now!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(1001, builder.build())
        }
    }
}
