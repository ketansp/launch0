package app.launch0.helper

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import app.launch0.data.CalendarEvent
import java.util.Calendar

/** Whether the user has granted read access to the device calendar. */
fun Context.hasCalendarPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Reads today's events straight from the device's calendar provider — entirely on-device, no
 * network. Recurring events are expanded via [CalendarContract.Instances]. Only events that
 * haven't finished yet (plus any all-day events) are returned, sorted so all-day events lead and
 * the rest follow in start order — i.e. "the rest of today", which is what the widget shows.
 *
 * Returns an empty list when the permission is missing or the query fails, so callers can treat an
 * empty result as "nothing to show" without special-casing errors.
 */
fun Context.getTodaysCalendarEvents(): List<CalendarEvent> {
    if (!hasCalendarPermission()) return emptyList()

    val dayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dayEnd = dayStart + ONE_DAY_MILLIS
    val now = System.currentTimeMillis()

    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
        ContentUris.appendId(this, dayStart)
        ContentUris.appendId(this, dayEnd)
    }.build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.EVENT_LOCATION,
    )

    val events = mutableListOf<CalendarEvent>()
    try {
        contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val locationIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)

            while (cursor.moveToNext()) {
                val allDay = cursor.getInt(allDayIdx) == 1
                val begin = cursor.getLong(beginIdx)
                val end = cursor.getLong(endIdx)
                // Keep only what's still ahead today; all-day events stay for the whole day.
                if (!allDay && end <= now) continue
                events.add(
                    CalendarEvent(
                        id = cursor.getLong(idIdx),
                        title = cursor.getString(titleIdx)?.trim().orEmpty(),
                        begin = begin,
                        end = end,
                        allDay = allDay,
                        location = cursor.getString(locationIdx)?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }

    return events.sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin })
}

private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
