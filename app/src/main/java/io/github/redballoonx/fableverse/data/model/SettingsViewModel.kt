package io.github.redballoonx.fableverse.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.redballoonx.fableverse.data.repository.AudiobookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val audiobookRepository: AudiobookRepository
) : ViewModel() {

    private val _selectedFolderUri = MutableStateFlow<String?>(null)
    val selectedFolderUri: StateFlow<String?> = _selectedFolderUri

    // Neuer State: Scan-Status
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult

    init {
        loadSavedFolder()
    }

    private fun loadSavedFolder() {
        viewModelScope.launch {
            _selectedFolderUri.value = audiobookRepository.getAudiobookFolderUri()
        }
    }

    fun onFolderSelected(uriString: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResult.value = null

            // Speichern
            audiobookRepository.saveAudiobookFolderUri(uriString)
            _selectedFolderUri.value = uriString

            // Scannen und in Datenbank speichern
            try {
                val bookCount = audiobookRepository.scanAndImportFolder(uriString)
                _scanResult.value = "$bookCount Hörbücher gefunden und importiert"
            } catch (e: Exception) {
                _scanResult.value = "Fehler beim Scannen: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }
}