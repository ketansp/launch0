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
 * a dedicated [SharedPreferences] file. Shared/added images are copied into an app-private
 * directory so they remain available even if the original is deleted from the device.
 *
 * All mutating calls do disk + IO work and should be invoked off the main thread.
 */
class NotesStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private val imagesDir: File
        get() = File(appContext.filesDir, IMAGES_DIR_NAME).apply { if (!exists()) mkdirs() }

    fun getEntries(): List<NotesEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<NotesEntry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val imagePath = obj.optString(KEY_IMAGE_PATH, "")
                list.add(
                    NotesEntry(
                        id = obj.getLong(KEY_ID),
                        type = obj.optString(KEY_TYPE, NotesEntry.TYPE_TEXT),
                        text = obj.optString(KEY_TEXT, ""),
                        imagePath = imagePath.ifEmpty { null },
                        timestamp = obj.optLong(KEY_TIMESTAMP, obj.getLong(KEY_ID)),
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

    @Synchronized
    fun delete(entry: NotesEntry) {
        persist(getEntries().filterNot { it.id == entry.id })
        val path = entry.imagePath ?: return
        val file = File(path)
        // Only ever delete files we own inside the notes images directory.
        if (file.exists() && file.parentFile?.absolutePath == imagesDir.absolutePath) file.delete()
    }

    private fun persist(entries: List<NotesEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put(KEY_ID, entry.id)
            obj.put(KEY_TYPE, entry.type)
            obj.put(KEY_TEXT, entry.text)
            obj.put(KEY_IMAGE_PATH, entry.imagePath ?: "")
            obj.put(KEY_TIMESTAMP, entry.timestamp)
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
        private const val KEY_ENTRIES = "NOTES_ENTRIES"
        private const val KEY_ID = "id"
        private const val KEY_TYPE = "type"
        private const val KEY_TEXT = "text"
        private const val KEY_IMAGE_PATH = "imagePath"
        private const val KEY_TIMESTAMP = "timestamp"
    }
}
