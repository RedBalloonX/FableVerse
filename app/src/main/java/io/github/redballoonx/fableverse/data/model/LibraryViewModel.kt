package io.github.redballoonx.fableverse.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.redballoonx.fableverse.data.model.Audiobook
import io.github.redballoonx.fableverse.data.model.AuthorWithBooks
import io.github.redballoonx.fableverse.data.repository.AudiobookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val audiobookRepository: AudiobookRepository
) : ViewModel() {

    // Alle Hörbücher (alphabetisch)
    private val _allAudiobooks = MutableStateFlow<List<Audiobook>>(emptyList())
    val allAudiobooks: StateFlow<List<Audiobook>> = _allAudiobooks

    // Nach Autoren gruppiert
    private val _authorGroups = MutableStateFlow<List<AuthorWithBooks>>(emptyList())
    val authorGroups: StateFlow<List<AuthorWithBooks>> = _authorGroups

    // Weiterhören
    val continueListening = audiobookRepository.getContinueListeningFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Zuletzt gespielt
    val recentlyPlayed = audiobookRepository.getRecentlyPlayedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Welche Autoren sind ausgeklappt
    private val _expandedAuthors = MutableStateFlow<Set<Long>>(emptySet())
    val expandedAuthors: StateFlow<Set<Long>> = _expandedAuthors

    // Suchfeld
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Gefilterte Ergebnisse
    private val _filteredBooks = MutableStateFlow<List<Audiobook>>(emptyList())
    val filteredBooks: StateFlow<List<Audiobook>> = _filteredBooks

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            loadAllAudiobooks()
            loadAuthorGroups()
        }
    }

    private suspend fun loadAllAudiobooks() {
        _allAudiobooks.value = audiobookRepository.getAllAudiobooks()
    }

    private suspend fun loadAuthorGroups() {
        _authorGroups.value = audiobookRepository.getAudiobooksByAuthor()
    }

    fun refreshAudiobooks() {
        loadAllData()
    }

    fun toggleAuthor(authorId: Long) {
        _expandedAuthors.value = if (_expandedAuthors.value.contains(authorId)) {
            _expandedAuthors.value - authorId
        } else {
            _expandedAuthors.value + authorId
        }
    }

    fun expandAll() {
        _expandedAuthors.value = _authorGroups.value.map { it.authorId }.toSet()
    }

    fun collapseAll() {
        _expandedAuthors.value = emptySet()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterBooks(query)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _filteredBooks.value = emptyList()
    }

    private fun filterBooks(query: String) {
        if (query.isBlank()) {
            _filteredBooks.value = emptyList()
            return
        }

        _filteredBooks.value = _allAudiobooks.value.filter { book ->
            book.title.contains(query, ignoreCase = true) ||
                    book.author?.contains(query, ignoreCase = true) == true
        }
    }
}