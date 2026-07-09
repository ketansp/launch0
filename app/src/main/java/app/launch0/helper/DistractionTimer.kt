package app.launch0.helper

import app.launch0.data.Constants
import app.launch0.data.Prefs
import kotlin.math.min

/**
 * Logic for the distraction timer: apps the user marks as distracting open only after a wait.
 * In escalating mode the wait doubles with every open of that app today (10s → 20s → 40s → 60s,
 * capped); in fixed mode every open waits 30s. Opens are counted per app per calendar day and
 * reset at local midnight. Turning back mid-wait does not undo the attempt — the tap already
 * counted, so the next open still waits longer.
 */
object DistractionTimer {

    fun isDistractingApp(prefs: Prefs, packageName: String): Boolean =
        packageName.isNotEmpty() && prefs.distractionApps.contains(packageName)

    /** Opens of [packageName] counted so far today (attempts included). */
    fun opensToday(prefs: Prefs, packageName: String): Int {
        rolloverIfNeeded(prefs)
        return parseCounts(prefs.distractionOpenCounts)[packageName] ?: 0
    }

    /** Wait in seconds for an open when the app already has [opensToday] opens today. */
    fun waitSeconds(prefs: Prefs, opensToday: Int): Int {
        if (!prefs.distractionWaitEscalating) return Constants.DistractionTimer.FIXED_WAIT_SECONDS
        val shift = opensToday.coerceIn(0, 8)
        return min(
            Constants.DistractionTimer.BASE_WAIT_SECONDS shl shift,
            Constants.DistractionTimer.MAX_WAIT_SECONDS
        )
    }

    /** Wait in seconds the next open of [packageName] would incur. */
    fun nextWaitSeconds(prefs: Prefs, packageName: String): Int =
        waitSeconds(prefs, opensToday(prefs, packageName))

    /** Counts one open attempt of [packageName] toward today; the next wait doubles from here. */
    fun recordAttempt(prefs: Prefs, packageName: String) {
        rolloverIfNeeded(prefs)
        val counts = parseCounts(prefs.distractionOpenCounts).toMutableMap()
        counts[packageName] = (counts[packageName] ?: 0) + 1
        prefs.distractionOpenCounts = serializeCounts(counts)
    }

    /** Stamps [packageName] as opened now; drives the "Last open" ledger row. */
    fun recordLaunch(prefs: Prefs, packageName: String) {
        val lastOpens = parseLongs(prefs.distractionLastOpens).toMutableMap()
        lastOpens[packageName] = System.currentTimeMillis()
        prefs.distractionLastOpens = serializeLongs(lastOpens)
    }

    /** When [packageName] last actually opened (millis), or 0 if never recorded. */
    fun lastOpenMillis(prefs: Prefs, packageName: String): Long {
        return parseLongs(prefs.distractionLastOpens)[packageName] ?: 0L
    }

    private fun rolloverIfNeeded(prefs: Prefs) {
        val todayMidnight = System.currentTimeMillis().convertEpochToMidnight()
        if (prefs.distractionCountsDay == todayMidnight) return
        prefs.distractionCountsDay = todayMidnight
        prefs.distractionOpenCounts = ""
    }

    private fun parseCounts(raw: String): Map<String, Int> =
        raw.split(';').filter { it.contains(':') }.associate { entry ->
            val idx = entry.lastIndexOf(':')
            entry.substring(0, idx) to (entry.substring(idx + 1).toIntOrNull() ?: 0)
        }

    private fun serializeCounts(counts: Map<String, Int>): String =
        counts.entries.joinToString(";") { "${it.key}:${it.value}" }

    private fun parseLongs(raw: String): Map<String, Long> =
        raw.split(';').filter { it.contains(':') }.associate { entry ->
            val idx = entry.lastIndexOf(':')
            entry.substring(0, idx) to (entry.substring(idx + 1).toLongOrNull() ?: 0L)
        }

    private fun serializeLongs(values: Map<String, Long>): String =
        values.entries.joinToString(";") { "${it.key}:${it.value}" }
}
