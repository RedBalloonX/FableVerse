package io.github.redballoonx.fableverse.data.model

data class Audiobook(
    val id: Long = 0,
    val title: String,
    val folderUri: String,
    val author: String? = null,
    val authorId: Long? = null,
    val chapters: List<Chapter> = emptyList(),
    val coverUri: String? = null,
    val totalDuration: Long = 0,
    val currentPosition: Long = 0,
    val progress: Float = 0f,          // 0.0 - 1.0
    val isFinished: Boolean = false,
    val lastPlayed: Long? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

data class Chapter(
    val id: Long = 0,
    val title: String,
    val uri: String,
    val position: Int,
    val duration: Long = 0
)

// Für gruppierte Ansicht nach Autoren
data class AuthorWithBooks(
    val authorId: Long,
    val authorName: String,
    val audiobooks: List<Audiobook>,
    val bookCount: Int
)