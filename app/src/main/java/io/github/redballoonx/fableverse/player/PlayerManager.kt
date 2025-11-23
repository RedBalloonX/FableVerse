package io.github.redballoonx.fableverse.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.redballoonx.fableverse.data.model.Chapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Verwaltet ExoPlayer und Player-State
 */
class PlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex = 0

    fun initialize() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayerState(isPlaying = isPlaying)
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                        updatePlayerState(playbackSpeed = playbackParameters.speed)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // Nächstes Kapitel automatisch
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            currentChapterIndex++
                            updatePlayerState(currentChapterIndex = currentChapterIndex)
                        }
                    }
                })
            }
        }
    }

    fun loadAudiobook(
        chapterList: List<Chapter>,
        startChapterIndex: Int = 0,
        startPosition: Long = 0
    ) {
        chapters = chapterList
        currentChapterIndex = startChapterIndex

        player?.apply {
            // Alle Kapitel als Playlist hinzufügen
            clearMediaItems()
            val mediaItems = chapters.map { chapter ->
                MediaItem.fromUri(Uri.parse(chapter.uri))
            }
            setMediaItems(mediaItems, startChapterIndex, startPosition)
            prepare()

            updatePlayerState(
                currentChapterIndex = startChapterIndex,
                duration = 0, // Wird aktualisiert wenn geladen
                totalChapters = chapters.size
            )
        }
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun nextChapter() {
        player?.apply {
            if (hasNextMediaItem()) {
                seekToNextMediaItem()
                currentChapterIndex++
                updatePlayerState(currentChapterIndex = currentChapterIndex)
            }
        }
    }

    fun previousChapter() {
        player?.apply {
            if (hasPreviousMediaItem()) {
                seekToPreviousMediaItem()
                currentChapterIndex--
                updatePlayerState(currentChapterIndex = currentChapterIndex)
            }
        }
    }

    fun jumpToChapter(index: Int) {
        if (index in chapters.indices) {
            player?.apply {
                seekTo(index, 0)
                currentChapterIndex = index
                updatePlayerState(currentChapterIndex = index)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }

    fun getDuration(): Long {
        return player?.duration ?: 0
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    fun getCurrentChapterIndex(): Int {
        return currentChapterIndex
    }

    fun release() {
        player?.release()
        player = null
    }

    private fun updatePlayerState(
        isPlaying: Boolean = _playerState.value.isPlaying,
        currentChapterIndex: Int = _playerState.value.currentChapterIndex,
        position: Long = player?.currentPosition ?: 0,
        duration: Long = player?.duration ?: 0,
        playbackSpeed: Float = _playerState.value.playbackSpeed,
        totalChapters: Int = _playerState.value.totalChapters
    ) {
        _playerState.value = PlayerState(
            isPlaying = isPlaying,
            currentChapterIndex = currentChapterIndex,
            position = position,
            duration = duration,
            playbackSpeed = playbackSpeed,
            totalChapters = totalChapters
        )
    }
}

/**
 * Aktueller Player-Zustand
 */
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentChapterIndex: Int = 0,
    val position: Long = 0,
    val duration: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val totalChapters: Int = 0
)