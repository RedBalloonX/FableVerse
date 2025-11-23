package io.github.redballoonx.fableverse.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.redballoonx.fableverse.data.model.Audiobook
import io.github.redballoonx.fableverse.data.repository.AudiobookRepository
import io.github.redballoonx.fableverse.player.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val audiobookRepository: AudiobookRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _audiobook = MutableStateFlow<Audiobook?>(null)
    val audiobook: StateFlow<Audiobook?> = _audiobook

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes

    val playerState = playerManager.playerState

    private var progressUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    fun loadAudiobook(audiobookId: Long) {
        viewModelScope.launch {
            val book = audiobookRepository.getAudiobookById(audiobookId)
            if (book != null) {
                _audiobook.value = book

                // Lade im Player
                playerManager.initialize()
                playerManager.loadAudiobook(
                    chapterList = book.chapters,
                    startChapterIndex = book.currentChapterIndex ?: 0,
                    startPosition = book.currentPosition
                )

                // Setze Playback-Speed
                playerManager.setPlaybackSpeed(book.playbackSpeed ?: 1.0f)

                // Starte Progress-Updates
                startProgressUpdates()
            }
        }
    }

    fun togglePlayPause() {
        if (playerManager.isPlaying()) {
            playerManager.pause()
        } else {
            playerManager.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
        saveProgress()
    }

    fun nextChapter() {
        playerManager.nextChapter()
        saveProgress()
    }

    fun previousChapter() {
        playerManager.previousChapter()
        saveProgress()
    }

    fun jumpToChapter(index: Int) {
        playerManager.jumpToChapter(index)
        saveProgress()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)

        viewModelScope.launch {
            _audiobook.value?.let { book ->
                audiobookRepository.updatePlaybackSpeed(book.id, speed)
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes

        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)

                // Speichere in DB
                _audiobook.value?.let { book ->
                    audiobookRepository.setSleepTimer(book.id, endTime)
                }

                // Countdown
                repeat(minutes * 60) {
                    delay(1000)
                    val remainingMs = endTime - System.currentTimeMillis()
                    _sleepTimerMinutes.value = (remainingMs / 60000).toInt()

                    if (remainingMs <= 0) {
                        playerManager.pause()
                        _sleepTimerMinutes.value = null
                        return@launch
                    }
                }
            }
        } else {
            // Timer abbrechen
            _audiobook.value?.let { book ->
                viewModelScope.launch {
                    audiobookRepository.setSleepTimer(book.id, null)
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Jede Sekunde aktualisieren
                saveProgress()
            }
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            _audiobook.value?.let { book ->
                audiobookRepository.updatePlaybackProgress(
                    audiobookId = book.id,
                    chapterIndex = playerManager.getCurrentChapterIndex(),
                    positionMs = playerManager.getCurrentPosition()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressUpdateJob?.cancel()
        sleepTimerJob?.cancel()
        playerManager.release()
    }
}