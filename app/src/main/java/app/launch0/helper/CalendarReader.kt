package app.launch0.helper

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import app.launch0.data.CalendarEvent
import java.util.Calendar

/** Whether the user has granted read access to the device calendar. */
fun Context.hasCalendarPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Reads the whole of today's events straight from the device's calendar provider — entirely
 * on-device, no network. Recurring events are expanded via [CalendarContract.Instances]. Every
 * event for the day is returned (the widget's list scrolls through them), sorted so all-day events
 * lead and the rest follow in start order.
 *
 * Both the personal profile and — where a work profile shares its calendar — the managed profile
 * are queried, since corporate meetings often live in the work profile the launcher can't otherwise
 * see. Identical instances (same title, time and all-day flag) are collapsed so an event synced into
 * two calendars or both profiles shows once.
 *
 * Returns an empty list when the permission is missing or the query fails, so callers can treat an
 * empty result as "nothing to show" without special-casing errors.
 */
fun Context.getTodaysCalendarEvents(): List<CalendarEvent> {
    if (!hasCalendarPermission()) return emptyList()

    val dayStart = todayStartMillis()
    val dayEnd = dayStart + ONE_DAY_MILLIS

    val events = mutableListOf<CalendarEvent>()
    events += queryDayInstances(CalendarContract.Instances.CONTENT_URI, dayStart, dayEnd)
    // Work-profile events, when the managed profile is set to share its calendar across profiles.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        events += queryDayInstances(CalendarContract.Instances.ENTERPRISE_CONTENT_URI, dayStart, dayEnd)

    return events
        .distinctBy { listOf(it.title, it.begin, it.end, it.allDay) }
        .sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin })
}

/** Queries one instances URI (personal or work profile) for the [dayStart]..[dayEnd] window. */
private fun Context.queryDayInstances(base: Uri, dayStart: Long, dayEnd: Long): List<CalendarEvent> {
    val uri = base.buildUpon().apply {
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
                events.add(
                    CalendarEvent(
                        id = cursor.getLong(idIdx),
                        title = cursor.getString(titleIdx)?.trim().orEmpty(),
                        begin = cursor.getLong(beginIdx),
                        end = cursor.getLong(endIdx),
                        allDay = cursor.getInt(allDayIdx) == 1,
                        location = cursor.getString(locationIdx)?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            }
        }
    } catch (e: Exception) {
        // A missing provider or a work profile that doesn't share its calendar just yields nothing.
        e.printStackTrace()
    }
    return events
}

private fun todayStartMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

// ---- Debug-only diagnostics (not committed): where are today's events actually coming from? ----

/** A short human-readable breakdown of today's calendar sources, for a debug toast. */
fun Context.calendarDiagnostics(): String {
    if (!hasCalendarPermission()) return "calendar: no permission"
    val dayStart = todayStartMillis()
    val dayEnd = dayStart + ONE_DAY_MILLIS
    val personal = queryDayInstances(CalendarContract.Instances.CONTENT_URI, dayStart, dayEnd).size
    val work = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        queryDayInstances(CalendarContract.Instances.ENTERPRISE_CONTENT_URI, dayStart, dayEnd).size else -1
    val cals = queryCalendarLabels()
    return "today: personal=$personal, work=$work\ncalendars (${cals.size}):\n" + cals.joinToString("\n")
}

/** Lists the device's calendars (name + visibility + today's event count) for diagnostics. */
private fun Context.queryCalendarLabels(): List<String> {
    val labels = mutableListOf<String>()
    val dayStart = todayStartMillis()
    val dayEnd = dayStart + ONE_DAY_MILLIS
    try {
        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.VISIBLE,
            ),
            null, null, null,
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val visIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx) ?: "?"
                val vis = if (c.getInt(visIdx) == 1) "" else " (hidden)"
                labels.add("• $name$vis: ${countTodayEventsForCalendar(id, dayStart, dayEnd)}")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return labels
}

private fun Context.countTodayEventsForCalendar(calendarId: Long, dayStart: Long, dayEnd: Long): Int {
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
        ContentUris.appendId(this, dayStart)
        ContentUris.appendId(this, dayEnd)
    }.build()
    return try {
        contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.EVENT_ID),
            "${CalendarContract.Instances.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString()),
            null,
        )?.use { it.count } ?: 0
    } catch (e: Exception) {
        e.printStackTrace()
        -1
    }
}

private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
