package app.launch0.data

/**
 * A single item on the personal notes page.
 *
 * The notes is a "chat with yourself" — every note or shared image becomes one [NotesEntry].
 * Entries are persisted as JSON via [NotesStore]; images are copied into app-internal storage
 * and referenced here by their absolute file path.
 */
data class NotesEntry(
    val id: Long,
    val type: String,
    val text: String,
    val imagePath: String?,
    val timestamp: Long,
) {
    val isImage: Boolean get() = type == TYPE_IMAGE

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
    }
}
