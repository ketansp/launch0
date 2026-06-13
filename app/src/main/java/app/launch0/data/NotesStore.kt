package app.launch0.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistence for the personal notes page.
 *
 * Following the project convention of "no database", entries are stored as a JSON array string in
 * a dedicated [SharedPreferences] file. Shared/added images and recorded voice notes are copied
 * into app-private directories so they remain available even if the original is deleted.
 *
 * All mutating calls do disk + IO work and should be invoked off the main thread.
 */
class NotesStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private val imagesDir: File
        get() = File(appContext.filesDir, IMAGES_DIR_NAME).apply { if (!exists()) mkdirs() }

    private val audioDir: File
        get() = File(appContext.filesDir, AUDIO_DIR_NAME).apply { if (!exists()) mkdirs() }

    fun getEntries(): List<NotesEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<NotesEntry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mediaPath = obj.optString(KEY_MEDIA_PATH, "")
                list.add(
                    NotesEntry(
                        id = obj.getLong(KEY_ID),
                        type = obj.optString(KEY_TYPE, NotesEntry.TYPE_TEXT),
                        text = obj.optString(KEY_TEXT, ""),
                        mediaPath = mediaPath.ifEmpty { null },
                        timestamp = obj.optLong(KEY_TIMESTAMP, obj.getLong(KEY_ID)),
                        done = obj.optBoolean(KEY_DONE, false),
                        urgent = obj.optBoolean(KEY_URGENT, false),
                        durationMs = obj.optLong(KEY_DURATION, 0L),
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Synchronized
    fun addText(text: String): NotesEntry? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val now = System.currentTimeMillis()
        val entry = NotesEntry(now, NotesEntry.TYPE_TEXT, trimmed, null, now)
        persist(getEntries() + entry)
        return entry
    }

    @Synchronized
    fun addImageFromUri(uri: Uri): NotesEntry? {
        val now = uniqueTimestamp()
        val file = File(imagesDir, "img_$now.${guessExtension(uri)}")
        return try {
            val copied = appContext.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied || file.length() == 0L) {
                if (file.exists()) file.delete()
                return null
            }
            val entry = NotesEntry(now, NotesEntry.TYPE_IMAGE, "", file.absolutePath, now)
            persist(getEntries() + entry)
            entry
        } catch (e: Exception) {
            e.printStackTrace()
            if (file.exists()) file.delete()
            null
        }
    }

    /** A fresh file in the audio directory for a recorder to write into. */
    fun newAudioFile(): File = File(audioDir, "audio_${uniqueTimestamp()}.m4a")

    @Synchronized
    fun addAudio(file: File, durationMs: Long): NotesEntry? {
        if (!file.exists() || file.length() == 0L) {
            if (file.exists()) file.delete()
            return null
        }
        val now = System.currentTimeMillis()
        val entry = NotesEntry(now, NotesEntry.TYPE_AUDIO, "", file.absolutePath, now, durationMs = durationMs)
        persist(getEntries() + entry)
        return entry
    }

    /** Replaces the stored entry that shares [updated]'s id, leaving any media file untouched. */
    @Synchronized
    fun update(updated: NotesEntry) {
        persist(getEntries().map { if (it.id == updated.id) updated else it })
    }

    @Synchronized
    fun delete(entry: NotesEntry) {
        persist(getEntries().filterNot { it.id == entry.id })
        val path = entry.mediaPath ?: return
        val file = File(path)
        // Only ever delete files we own inside our own media directories.
        val parent = file.parentFile?.absolutePath
        val owned = parent == imagesDir.absolutePath || parent == audioDir.absolutePath
        if (file.exists() && owned) file.delete()
    }

    private fun persist(entries: List<NotesEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put(KEY_ID, entry.id)
            obj.put(KEY_TYPE, entry.type)
            obj.put(KEY_TEXT, entry.text)
            obj.put(KEY_MEDIA_PATH, entry.mediaPath ?: "")
            obj.put(KEY_TIMESTAMP, entry.timestamp)
            obj.put(KEY_DONE, entry.done)
            obj.put(KEY_URGENT, entry.urgent)
            obj.put(KEY_DURATION, entry.durationMs)
            array.put(obj)
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    /** Avoid two rapid adds colliding on the same millisecond-based id. */
    private fun uniqueTimestamp(): Long {
        var now = System.currentTimeMillis()
        val existing = getEntries().map { it.id }.toSet()
        while (existing.contains(now)) now++
        return now
    }

    private fun guessExtension(uri: Uri): String {
        return when (appContext.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }

    companion object {
        private const val PREFS_FILENAME = "app.launch0.notes"
        private const val IMAGES_DIR_NAME = "notes_images"
        private const val AUDIO_DIR_NAME = "notes_audio"
        private const val KEY_ENTRIES = "NOTES_ENTRIES"
        private const val KEY_ID = "id"
        private const val KEY_TYPE = "type"
        private const val KEY_TEXT = "text"
        private const val KEY_MEDIA_PATH = "imagePath"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_DONE = "done"
        private const val KEY_URGENT = "urgent"
        private const val KEY_DURATION = "duration"
    }
}
