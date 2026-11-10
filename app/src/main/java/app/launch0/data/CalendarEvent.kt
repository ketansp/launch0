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
    /**
     * The event's [android.provider.CalendarContract.Instances.STATUS] (e.g.
     * [android.provider.CalendarContract.Instances.STATUS_CANCELED]), or null when the provider
     * leaves it unset. Cancelled instances — including the "ghost" left behind when a recurring
     * occurrence is rescheduled — are filtered out before display.
     */
    val status: Int? = null,
    /**
     * The current user's response, from
     * [android.provider.CalendarContract.Instances.SELF_ATTENDEE_STATUS] (e.g.
     * [android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED]). Declined events are
     * filtered out before display, matching the Google Calendar app.
     */
    val selfAttendeeStatus: Int = android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_NONE,
) {
    /** True while the event is currently happening. */
    fun isNow(now: Long): Boolean = now in begin until end
}
