package com.tarzo.ai.features.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// ── Entity ────────────────────────────────────────────────────────────

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val time: Long,
    val type: ReminderType,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReminderType {
    ALARM,
    TIMER,
    REMINDER;

    companion object {
        fun fromString(value: String): ReminderType {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                REMINDER
            }
        }
    }
}

/**
 * Display model for the UI layer.
 */
data class ReminderItem(
    val id: Long,
    val title: String,
    val time: Long,
    val type: ReminderType,
    val isEnabled: Boolean,
    val isExpired: Boolean
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(time))
    }

    fun timeUntil(): String {
        val diff = time - System.currentTimeMillis()
        return when {
            diff <= 0 -> "Expired"
            diff < 60_000 -> "In ${diff / 1000} seconds"
            diff < 3_600_000 -> "In ${diff / 60_000} minutes"
            diff < 86_400_000 -> "In ${diff / 3_600_000} hours"
            else -> "In ${diff / 86_400_000} days"
        }
    }
}

// ── DAO ───────────────────────────────────────────────────────────────

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE type = :type ORDER BY time ASC")
    fun getByType(type: ReminderType): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT COUNT(*) FROM reminders WHERE isEnabled = 1")
    suspend fun getActiveCount(): Int

    @Query("DELETE FROM reminders WHERE type = :type AND time < :beforeTime")
    suspend fun deleteExpiredByType(type: ReminderType, beforeTime: Long)
}

// ── Type Converters ───────────────────────────────────────────────────

class Converters {
    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.fromString(value)
}

// ── BroadcastReceiver ─────────────────────────────────────────────────

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        val type = intent.getStringExtra(EXTRA_TYPE) ?: ReminderType.REMINDER.name

        showNotification(context, title, type, reminderId)
    }

    private fun showNotification(context: Context, title: String, type: String, reminderId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tarzo_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TARZO Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarms, timers, and reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = reminderId.toInt().coerceAtLeast(1)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Your ${type.lowercase()} is due now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TYPE = "reminder_type"
    }
}

// ── Manager ───────────────────────────────────────────────────────────

/**
 * Manages alarms, timers, and reminders using [AlarmManager] and local Room storage.
 * Each reminder is stored as a [ReminderEntity] and scheduled via a [PendingIntent]
 * that triggers [ReminderReceiver] at the specified time.
 */
@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Creates an alarm at the specified time.
     *
     * @param time The alarm trigger time in milliseconds since epoch.
     * @param label A descriptive label for the alarm.
     * @return [Result] with the created reminder ID.
     */
    suspend fun createAlarm(time: Long, label: String): Result<Long> {
        return createReminderInternal(time, label, ReminderType.ALARM)
    }

    /**
     * Creates a timer that fires after the specified duration.
     *
     * @param durationMinutes The timer duration in minutes.
     * @param label A descriptive label for the timer.
     * @return [Result] with the created reminder ID.
     */
    suspend fun createTimer(durationMinutes: Int, label: String): Result<Long> {
        val triggerTime = System.currentTimeMillis() + durationMinutes * 60_000L
        return createReminderInternal(triggerTime, label, ReminderType.TIMER)
    }

    /**
     * Creates a reminder at the specified time.
     *
     * @param time The reminder trigger time in milliseconds since epoch.
     * @param label A descriptive label for the reminder.
     * @return [Result] with the created reminder ID.
     */
    suspend fun createReminder(time: Long, label: String): Result<Long> {
        return createReminderInternal(time, label, ReminderType.REMINDER)
    }

    /**
     * Lists all reminders as a [Flow] of [ReminderItem] for UI observation.
     */
    fun listReminders(): Flow<List<ReminderItem>> {
        return reminderDao.getAllReminders().mapToListItems()
    }

    /**
     * Deletes a reminder by ID and cancels its scheduled alarm.
     */
    suspend fun deleteReminder(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            reminderDao.deleteById(id)
            cancelAlarm(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete reminder: ${e.message}")
        }
    }

    /**
     * Cancels the scheduled alarm for a reminder without deleting the database entry.
     */
    suspend fun cancelAlarm(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.toInt().coerceAtLeast(1),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            reminderDao.setEnabled(id, false)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to cancel alarm: ${e.message}")
        }
    }

    // ── Internal ───────────────────────────────────────────────────────

    private suspend fun createReminderInternal(
        time: Long,
        label: String,
        type: ReminderType
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val entity = ReminderEntity(
                title = label.ifBlank { "TARZO ${type.name.lowercase()}" },
                time = time,
                type = type
            )
            val id = reminderDao.insert(entity)

            if (id > 0) {
                scheduleAlarm(id, entity.title, entity.time, entity.type)
                val formattedTime = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(time))
                Result.Success(id)
            } else {
                Result.Error(
                    IllegalStateException("Database insert returned invalid ID"),
                    "Failed to create ${type.name.lowercase()}."
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to create ${type.name.lowercase()}: ${e.message}")
        }
    }

    private fun scheduleAlarm(id: Long, title: String, time: Long, type: ReminderType) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
            putExtra(ReminderReceiver.EXTRA_TYPE, type.name)
        }

        val requestCode = id.toInt().coerceAtLeast(1)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = time
        val triggerInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(triggerInfo, pendingIntent)
    }

    private fun Flow<List<ReminderEntity>>.mapToListItems(): Flow<List<ReminderItem>> {
        val now = System.currentTimeMillis()
        return this.map { entities ->
            entities.map { entity ->
                ReminderItem(
                    id = entity.id,
                    title = entity.title,
                    time = entity.time,
                    type = entity.type,
                    isEnabled = entity.isEnabled && entity.time > now,
                    isExpired = entity.time <= now
                )
            }
        }
    }
}
