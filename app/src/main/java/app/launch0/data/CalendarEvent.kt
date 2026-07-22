package app.launch0.data

/**
 * A single calendar event shown in the home-screen calendar widget. Read locally from the
 * device's [android.provider.CalendarContract] provider — see
 * [app.launch0.helper.getTodaysCalendarEvents]. Recurring events are already expanded into
 * concrete instances, so [begin]/[end] are absolute times for this occurrence.
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
) {
    /** True while the event is currently happening. */
    fun isNow(now: Long): Boolean = now in begin until end
}
