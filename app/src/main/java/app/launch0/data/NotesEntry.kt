package app.launch0.data

/**
 * A single item on the personal notes page.
 *
 * The notes is a "chat with yourself" — every note, shared image or voice memo becomes one
 * [NotesEntry]. Entries are persisted as JSON via [NotesStore]; images and audio are copied into
 * app-internal storage and referenced here by their absolute file path ([mediaPath]).
 *
 * Text entries double as to-dos: [done] marks them complete and [urgent] flags them as important.
 */
data class NotesEntry(
    val id: Long,
    val type: String,
    val text: String,
    val mediaPath: String?,
    val timestamp: Long,
    val done: Boolean = false,
    val urgent: Boolean = false,
    val durationMs: Long = 0L,
) {
    val isImage: Boolean get() = type == TYPE_IMAGE
    val isAudio: Boolean get() = type == TYPE_AUDIO
    val isText: Boolean get() = type == TYPE_TEXT

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_AUDIO = "audio"
    }
}
