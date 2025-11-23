package io.github.redballoonx.fableverse.data.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Extrahiert Metadaten aus MP3-Dateien
 */
class MetadataExtractor(private val context: Context) {

    /**
     * Extrahiert Metadaten aus einer MP3-Datei
     */
    suspend fun extractMetadata(uri: Uri): AudioFileMetadata? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                AudioFileMetadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                    year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull(),
                    trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull(),
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                )
            } catch (e: Exception) {
                Log.e("MetadataExtractor", "Error extracting metadata from $uri", e)
                null
            } finally {
                retriever.release()
            }
        }
    }

    /**
     * Extrahiert Cover-Bild und speichert es lokal
     * @return URI zum gespeicherten Cover oder null
     */
    suspend fun extractAndSaveCover(uri: Uri, audiobookId: Long): String? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                // Extrahiere embedded Cover
                val coverBytes = retriever.embeddedPicture
                if (coverBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                    if (bitmap != null) {
                        // Speichere Cover lokal
                        saveCoverToFile(bitmap, audiobookId)
                    } else null
                } else null
            } catch (e: Exception) {
                Log.e("MetadataExtractor", "Error extracting cover from $uri", e)
                null
            } finally {
                retriever.release()
            }
        }
    }

    /**
     * Speichert Cover-Bild als Datei
     */
    private fun saveCoverToFile(bitmap: Bitmap, audiobookId: Long): String? {
        return try {
            // Erstelle Cover-Ordner
            val coverDir = File(context.filesDir, "covers")
            if (!coverDir.exists()) {
                coverDir.mkdirs()
            }

            // Speichere als JPEG
            val coverFile = File(coverDir, "cover_$audiobookId.jpg")
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Gib URI zurück
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e("MetadataExtractor", "Error saving cover", e)
            null
        }
    }

    /**
     * Extrahiert Metadaten aus allen MP3s eines Hörbuchs
     * und versucht gemeinsame Metadaten zu finden
     */
    suspend fun extractAudiobookMetadata(mp3Uris: List<Uri>): AudiobookMetadata {
        val metadataList = mp3Uris.mapNotNull { extractMetadata(it) }

        if (metadataList.isEmpty()) {
            return AudiobookMetadata()
        }

        // Finde häufigsten Wert für jedes Feld
        return AudiobookMetadata(
            title = findMostCommon(metadataList.mapNotNull { it.album }),
            artist = findMostCommon(metadataList.mapNotNull { it.albumArtist ?: it.artist }),
            album = findMostCommon(metadataList.mapNotNull { it.album }),
            genre = findMostCommon(metadataList.mapNotNull { it.genre }),
            year = metadataList.mapNotNull { it.year }.firstOrNull(),
            totalDuration = metadataList.sumOf { it.duration }
        )
    }

    /**
     * Findet den häufigsten Wert in einer Liste
     */
    private fun findMostCommon(values: List<String>): String? {
        return values
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }
}

/**
 * Metadaten einer einzelnen MP3-Datei
 */
data class AudioFileMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val duration: Long = 0L
)

/**
 * Aggregierte Metadaten für ein komplettes Hörbuch
 */
data class AudiobookMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val totalDuration: Long = 0L
)