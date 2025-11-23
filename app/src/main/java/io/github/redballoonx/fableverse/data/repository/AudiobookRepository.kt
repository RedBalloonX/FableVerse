package io.github.redballoonx.fableverse.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import io.github.redballoonx.fableverse.data.local.AudiobookDatabase
import io.github.redballoonx.fableverse.data.local.PreferencesManager
import io.github.redballoonx.fableverse.data.local.entity.AudiobookEntity
import io.github.redballoonx.fableverse.data.local.entity.AuthorEntity
import io.github.redballoonx.fableverse.data.local.entity.ChapterEntity
import io.github.redballoonx.fableverse.data.metadata.MetadataExtractor
import io.github.redballoonx.fableverse.data.model.Audiobook       // ← FEHLTE
import io.github.redballoonx.fableverse.data.model.AuthorWithBooks // ← FEHLTE
import io.github.redballoonx.fableverse.data.model.Chapter         // ← FEHLTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
class AudiobookRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val database: AudiobookDatabase,
    private val metadataExtractor: MetadataExtractor  // ← NEU
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

            // 4. Speichere Hörbücher und Kapitel MIT METADATEN
            var totalBooks = 0
            scannedData.forEach { (authorName, books) ->
                val authorId = authorMap[authorName] ?: return@forEach

                books.forEach { scannedBook ->
                    // NEU: Extrahiere Metadaten aus allen MP3s
                    val mp3Uris = scannedBook.chapters.map { it.uri.toUri() }
                    val audiobookMetadata = metadataExtractor.extractAudiobookMetadata(mp3Uris)

                    // Speichere Hörbuch
                    val audiobookId = dao.insertAudiobook(
                        AudiobookEntity(
                            title = audiobookMetadata.title
                                ?: scannedBook.title,  // Metadaten haben Vorrang
                            folderUri = scannedBook.folderUri,
                            authorId = authorId,
                            albumTitle = audiobookMetadata.album,
                            artist = audiobookMetadata.artist,
                            album = audiobookMetadata.album,
                            genre = audiobookMetadata.genre,
                            year = audiobookMetadata.year,
                            totalDuration = audiobookMetadata.totalDuration
                        )
                    )

                    // NEU: Versuche Cover zu extrahieren (aus erster MP3)
                    if (mp3Uris.isNotEmpty()) {
                        val coverUri = metadataExtractor.extractAndSaveCover(
                            mp3Uris.first(),
                            audiobookId
                        )
                        if (coverUri != null) {
                            // Aktualisiere Hörbuch mit Cover-URI
                            dao.updateAudiobook(
                                dao.getAudiobookById(audiobookId)!!.copy(coverUri = coverUri)
                            )
                        }
                    }

                    // Speichere Kapitel MIT METADATEN
                    val chapterEntities = scannedBook.chapters.mapIndexed { index, chapter ->
                        // Extrahiere Metadaten für dieses Kapitel
                        val chapterMetadata = metadataExtractor.extractMetadata(chapter.uri.toUri())

                        ChapterEntity(
                            audiobookId = audiobookId,
                            title = chapterMetadata?.title ?: chapter.title,  // Metadaten haben Vorrang
                            uri = chapter.uri,
                            position = index,
                            duration = chapterMetadata?.duration ?: 0L,
                            trackNumber = chapterMetadata?.trackNumber,
                            artist = chapterMetadata?.artist,
                            album = chapterMetadata?.album
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
            currentChapterIndex = entity.currentChapterIndex,  // ← NEU
            playbackSpeed = entity.playbackSpeed,              // ← NEU
            progress = progress,
            isFinished = entity.isFinished,
            lastPlayed = entity.lastPlayed,
            dateAdded = entity.dateAdded
        )
    }

    // === Fortschritt speichern ===

    suspend fun updatePlaybackProgress(
        audiobookId: Long,
        chapterIndex: Int,
        positionMs: Long
    ) {
        withContext(Dispatchers.IO) {
            val audiobook = dao.getAudiobookById(audiobookId) ?: return@withContext
            dao.updateAudiobook(
                audiobook.copy(
                    currentChapterIndex = chapterIndex,
                    currentPosition = positionMs,
                    lastPlayed = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updatePlaybackSpeed(audiobookId: Long, speed: Float) {
        withContext(Dispatchers.IO) {
            val audiobook = dao.getAudiobookById(audiobookId) ?: return@withContext
            dao.updateAudiobook(audiobook.copy(playbackSpeed = speed))
        }
    }

    suspend fun setSleepTimer(audiobookId: Long, endTimeMs: Long?) {
        withContext(Dispatchers.IO) {
            val audiobook = dao.getAudiobookById(audiobookId) ?: return@withContext
            dao.updateAudiobook(audiobook.copy(sleepTimerEndTime = endTimeMs))
        }
    }

    suspend fun markAsFinished(audiobookId: Long) {
        withContext(Dispatchers.IO) {
            val audiobook = dao.getAudiobookById(audiobookId) ?: return@withContext
            dao.updateAudiobook(
                audiobook.copy(
                    isFinished = true,
                    currentPosition = 0,
                    currentChapterIndex = 0
                )
            )
        }
    }

    suspend fun getAudiobookById(audiobookId: Long): Audiobook? {
        return withContext(Dispatchers.IO) {
            val entity = dao.getAudiobookById(audiobookId)
            entity?.let { mapEntityToAudiobook(it) }
        }
    }
}