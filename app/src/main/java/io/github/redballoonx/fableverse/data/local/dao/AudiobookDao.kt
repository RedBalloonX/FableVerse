package io.github.redballoonx.fableverse.data.local.dao

import androidx.room.*
import io.github.redballoonx.fableverse.data.local.entity.AudiobookEntity
import io.github.redballoonx.fableverse.data.local.entity.AuthorEntity
import io.github.redballoonx.fableverse.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {

    // === Audiobooks ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudiobook(audiobook: AudiobookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudiobooks(audiobooks: List<AudiobookEntity>)

    @Update
    suspend fun updateAudiobook(audiobook: AudiobookEntity)

    @Delete
    suspend fun deleteAudiobook(audiobook: AudiobookEntity)

    @Query("SELECT * FROM audiobooks ORDER BY title ASC")
    fun getAllAudiobooksFlow(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks ORDER BY title ASC")
    suspend fun getAllAudiobooks(): List<AudiobookEntity>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    suspend fun getAudiobookById(id: Long): AudiobookEntity?

    @Query("SELECT * FROM audiobooks WHERE authorId = :authorId ORDER BY title ASC")
    suspend fun getAudiobooksByAuthor(authorId: Long): List<AudiobookEntity>

    @Query("SELECT * FROM audiobooks WHERE currentPosition > 0 AND isFinished = 0 ORDER BY lastPlayed DESC")
    fun getContinueListeningFlow(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedFlow(limit: Int = 10): Flow<List<AudiobookEntity>>

    @Query("DELETE FROM audiobooks")
    suspend fun deleteAllAudiobooks()

    // === Authors ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(author: AuthorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthors(authors: List<AuthorEntity>)

    @Query("SELECT * FROM authors ORDER BY name ASC")
    fun getAllAuthorsFlow(): Flow<List<AuthorEntity>>

    @Query("SELECT * FROM authors ORDER BY name ASC")
    suspend fun getAllAuthors(): List<AuthorEntity>

    @Query("SELECT * FROM authors WHERE id = :id")
    suspend fun getAuthorById(id: Long): AuthorEntity?

    @Query("SELECT * FROM authors WHERE name = :name LIMIT 1")
    suspend fun getAuthorByName(name: String): AuthorEntity?

    @Query("DELETE FROM authors")
    suspend fun deleteAllAuthors()

    // === Chapters ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE audiobookId = :audiobookId ORDER BY position ASC")
    suspend fun getChaptersByAudiobook(audiobookId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE audiobookId = :audiobookId ORDER BY position ASC")
    fun getChaptersByAudiobookFlow(audiobookId: Long): Flow<List<ChapterEntity>>

    @Query("DELETE FROM chapters WHERE audiobookId = :audiobookId")
    suspend fun deleteChaptersByAudiobook(audiobookId: Long)

    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()
}