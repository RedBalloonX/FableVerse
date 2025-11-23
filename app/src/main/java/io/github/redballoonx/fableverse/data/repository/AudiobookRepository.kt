package io.github.redballoonx.fableverse.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import io.github.redballoonx.fableverse.data.local.AudiobookDatabase
import io.github.redballoonx.fableverse.data.local.PreferencesManager
import io.github.redballoonx.fableverse.data.local.entity.AudiobookEntity
import io.github.redballoonx.fableverse.data.local.entity.AuthorEntity
import io.github.redballoonx.fableverse.data.local.entity.ChapterEntity
import io.github.redballoonx.fableverse.data.model.Audiobook
import io.github.redballoonx.fableverse.data.model.AuthorWithBooks
import io.github.redballoonx.fableverse.data.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AudiobookRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val database: AudiobookDatabase
) {

    private val dao = database.audiobookDao()

    // === Settings ===

    suspend fun getAudiobookFolderUri(): String? {
        return withContext(Dispatchers.IO) {
            preferencesManager.getAudiobookFolderUri()
        }
    }

    suspend fun saveAudiobookFolderUri(uriString: String) {
        withContext(Dispatchers.IO) {
            preferencesManager.saveAudiobookFolderUri(uriString)
        }
    }

    // === Scannen und Importieren ===

    suspend fun scanAndImportFolder(uriString: String): Int {
        return withContext(Dispatchers.IO) {
            // 1. Scanne Ordnerstruktur
            val scannedData = scanFolderStructure(uriString)

            // 2. Lösche alte Daten
            dao.deleteAllChapters()
            dao.deleteAllAudiobooks()
            dao.deleteAllAuthors()

            // 3. Speichere Autoren
            val authorMap = mutableMapOf<String, Long>()
            scannedData.keys.forEach { authorName ->
                val authorId = dao.insertAuthor(
                    AuthorEntity(
                        name = authorName,
                        bookCount = scannedData[authorName]?.size ?: 0
                    )
                )
                authorMap[authorName] = authorId
            }

            // 4. Speichere Hörbücher und Kapitel
            var totalBooks = 0
            scannedData.forEach { (authorName, books) ->
                val authorId = authorMap[authorName] ?: return@forEach

                books.forEach { scannedBook ->
                    val audiobookId = dao.insertAudiobook(
                        AudiobookEntity(
                            title = scannedBook.title,
                            folderUri = scannedBook.folderUri,
                            authorId = authorId
                        )
                    )

                    val chapterEntities = scannedBook.chapters.map { chapter ->
                        ChapterEntity(
                            audiobookId = audiobookId,
                            title = chapter.title,
                            uri = chapter.uri,
                            position = chapter.position
                        )
                    }
                    dao.insertChapters(chapterEntities)
                    totalBooks++
                }
            }

            totalBooks
        }
    }

    // Private Scan-Funktion (gibt Map<Autor, List<Audiobook>> zurück)
    private suspend fun scanFolderStructure(uriString: String): Map<String, List<Audiobook>> {
        val uri = uriString.toUri()
        val rootFolder = DocumentFile.fromTreeUri(context, uri)
            ?: return emptyMap()

        val result = mutableMapOf<String, MutableList<Audiobook>>()
        scanRecursive(rootFolder, result, null, 0)
        return result
    }

    private fun scanRecursive(
        folder: DocumentFile,
        result: MutableMap<String, MutableList<Audiobook>>,
        authorName: String?,
        depth: Int
    ) {
        val mp3Files = mutableListOf<DocumentFile>()
        val subFolders = mutableListOf<DocumentFile>()

        folder.listFiles().forEach { item ->
            when {
                item.isFile && item.name?.endsWith(".mp3", ignoreCase = true) == true -> {
                    mp3Files.add(item)
                }
                item.isDirectory -> {
                    subFolders.add(item)
                }
            }
        }

        // Wenn MP3s gefunden → Hörbuch erstellen
        if (mp3Files.isNotEmpty()) {
            val chapters = mp3Files.mapIndexed { index, file ->
                Chapter(
                    title = file.name ?: "Unbekannt",
                    uri = file.uri.toString(),
                    position = index
                )
            }.sortedBy { it.title }

            val audiobook = Audiobook(
                title = folder.name ?: "Unbekannt",
                folderUri = folder.uri.toString(),
                author = authorName,
                chapters = chapters
            )

            val author = authorName ?: "Unbekannter Autor"
            result.getOrPut(author) { mutableListOf() }.add(audiobook)
        }

        // Rekursiv durchsuchen
        subFolders.forEach { subFolder ->
            val nextAuthor = when (depth) {
                0 -> subFolder.name
                else -> authorName
            }
            scanRecursive(subFolder, result, nextAuthor, depth + 1)
        }
    }

    // === Daten aus Datenbank laden ===

    suspend fun getAllAudiobooks(): List<Audiobook> {
        return withContext(Dispatchers.IO) {
            val audiobookEntities = dao.getAllAudiobooks()
            audiobookEntities.map { entity ->
                mapEntityToAudiobook(entity)
            }
        }
    }

    fun getAllAudiobooksFlow(): Flow<List<Audiobook>> {
        return dao.getAllAudiobooksFlow().map { entities ->
            entities.map { mapEntityToAudiobook(it) }
        }
    }

    suspend fun getAudiobooksByAuthor(): List<AuthorWithBooks> {
        return withContext(Dispatchers.IO) {
            val authors = dao.getAllAuthors()
            authors.map { author ->
                val books = dao.getAudiobooksByAuthor(author.id)
                    .map { mapEntityToAudiobook(it) }
                    .sortedBy { it.title }

                AuthorWithBooks(
                    authorId = author.id,
                    authorName = author.name,
                    audiobooks = books,
                    bookCount = books.size
                )
            }.sortedBy { it.authorName }
        }
    }

    fun getContinueListeningFlow(): Flow<List<Audiobook>> {
        return dao.getContinueListeningFlow().map { entities ->
            entities.map { mapEntityToAudiobook(it) }
        }
    }

    fun getRecentlyPlayedFlow(limit: Int = 10): Flow<List<Audiobook>> {
        return dao.getRecentlyPlayedFlow(limit).map { entities ->
            entities.map { mapEntityToAudiobook(it) }
        }
    }

    // Hilfsfunktion: Entity → Model
    private suspend fun mapEntityToAudiobook(entity: AudiobookEntity): Audiobook {
        val author = dao.getAuthorById(entity.authorId)
        val chapters = dao.getChaptersByAudiobook(entity.id).map { chapterEntity ->
            Chapter(
                id = chapterEntity.id,
                title = chapterEntity.title,
                uri = chapterEntity.uri,
                position = chapterEntity.position,
                duration = chapterEntity.duration
            )
        }

        val progress = if (entity.totalDuration > 0) {
            entity.currentPosition.toFloat() / entity.totalDuration.toFloat()
        } else 0f

        return Audiobook(
            id = entity.id,
            title = entity.title,
            folderUri = entity.folderUri,
            author = author?.name,
            authorId = entity.authorId,
            chapters = chapters,
            coverUri = entity.coverUri,
            totalDuration = entity.totalDuration,
            currentPosition = entity.currentPosition,
            progress = progress,
            isFinished = entity.isFinished,
            lastPlayed = entity.lastPlayed,
            dateAdded = entity.dateAdded
        )
    }
}