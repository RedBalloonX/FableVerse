package io.github.redballoonx.fableverse.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.redballoonx.fableverse.data.model.Audiobook
import io.github.redballoonx.fableverse.data.model.AuthorWithBooks
import java.io.File

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onAudiobookClick: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header mit Suche
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bibliothek",
                    style = MaterialTheme.typography.headlineMedium
                )

                IconButton(onClick = { viewModel.refreshAudiobooks() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Suchfeld
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Suchen...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Löschen")
                        }
                    }
                },
                singleLine = true
            )
        }

        // Wenn Suche aktiv: Zeige Ergebnisse
        if (searchQuery.isNotEmpty()) {
            SearchResultsView(viewModel, onAudiobookClick)
        } else {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Zuletzt") },
                    icon = { Icon(Icons.Default.History, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Autoren") },
                    icon = { Icon(Icons.Default.Person, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Alle") },
                    icon = { Icon(Icons.Default.List, null) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> RecentlyPlayedView(viewModel, onAudiobookClick)
                1 -> AuthorsView(viewModel, onAudiobookClick)
                2 -> AllBooksView(viewModel, onAudiobookClick)
            }
        }
    }
}

// Tab 1: Zuletzt gespielt / Weiterhören
@Composable
fun RecentlyPlayedView(
    viewModel: LibraryViewModel,
    onAudiobookClick: (Long) -> Unit
) {
    val continueListening by viewModel.continueListening.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Wenn nichts zum Weiterhören oder Zuletzt gespielt
        if (continueListening.isEmpty() && recentlyPlayed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Noch keine Hörbücher gehört",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: Weiterhören
        if (continueListening.isNotEmpty()) {
            item {
                Text(
                    "Weiterhören",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(continueListening) { audiobook ->
                AudiobookCardWithProgress(
                    audiobook = audiobook,
                    onClick = { onAudiobookClick(audiobook.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Section: Zuletzt gespielt
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    "Zuletzt gespielt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentlyPlayed) { audiobook ->
                AudiobookListItem(
                    audiobook = audiobook,
                    onClick = { onAudiobookClick(audiobook.id) }
                )
            }
        }
    }
}

// Tab 2: Nach Autoren gruppiert
@Composable
fun AuthorsView(
    viewModel: LibraryViewModel,
    onAudiobookClick: (Long) -> Unit
) {
    val authorGroups by viewModel.authorGroups.collectAsState()
    val expandedAuthors by viewModel.expandedAuthors.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Alle ausklappen/einklappen Buttons
        if (authorGroups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.expandAll() }) {
                    Text("Alle ausklappen")
                }
                TextButton(onClick = { viewModel.collapseAll() }) {
                    Text("Alle einklappen")
                }
            }
        }

        if (authorGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Keine Hörbücher gefunden",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Wähle einen Ordner in den Einstellungen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                authorGroups.forEach { authorGroup ->
                    // Autor-Header
                    item(key = "author_${authorGroup.authorId}") {
                        AuthorHeader(
                            author = authorGroup.authorName,
                            bookCount = authorGroup.bookCount,
                            isExpanded = expandedAuthors.contains(authorGroup.authorId),
                            onToggle = { viewModel.toggleAuthor(authorGroup.authorId) }
                        )
                    }

                    // Bücher (wenn ausgeklappt)
                    if (expandedAuthors.contains(authorGroup.authorId)) {
                        items(
                            items = authorGroup.audiobooks,
                            key = { it.id }
                        ) { audiobook ->
                            AudiobookListItem(
                                audiobook = audiobook,
                                onClick = { onAudiobookClick(audiobook.id) },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tab 3: Alle Bücher alphabetisch
@Composable
fun AllBooksView(
    viewModel: LibraryViewModel,
    onAudiobookClick: (Long) -> Unit
) {
    val allBooks by viewModel.allAudiobooks.collectAsState()

    if (allBooks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Keine Hörbücher gefunden",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Wähle einen Ordner in den Einstellungen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = allBooks,
                key = { it.id }
            ) { audiobook ->
                AudiobookListItem(
                    audiobook = audiobook,
                    onClick = { onAudiobookClick(audiobook.id) }
                )
            }
        }
    }
}

// Such-Ergebnisse
@Composable
fun SearchResultsView(
    viewModel: LibraryViewModel,
    onAudiobookClick: (Long) -> Unit
) {
    val filteredBooks by viewModel.filteredBooks.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filteredBooks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keine Ergebnisse gefunden",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    "${filteredBooks.size} ${if (filteredBooks.size == 1) "Ergebnis" else "Ergebnisse"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = filteredBooks,
                key = { it.id }
            ) { audiobook ->
                AudiobookListItem(
                    audiobook = audiobook,
                    onClick = { onAudiobookClick(audiobook.id) }
                )
            }
        }
    }
}

// === UI-Komponenten ===

// Autor-Header (ausklappbar)
@Composable
fun AuthorHeader(
    author: String,
    bookCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = author,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$bookCount ${if (bookCount == 1) "Buch" else "Bücher"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Einklappen" else "Ausklappen"
            )
        }
    }
}

// Hörbuch-Item (normale Liste)
@Composable
fun AudiobookListItem(
    audiobook: Audiobook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover-Bild oder Placeholder
            if (audiobook.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(audiobook.coverUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audiobook.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    audiobook.author?.let { author ->
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${audiobook.chapters.size} Kapitel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Zeige Gesamt-Dauer
                    if (audiobook.totalDuration > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(audiobook.totalDuration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Hörbuch-Card mit Fortschrittsbalken (für "Weiterhören")
@Composable
fun AudiobookCardWithProgress(
    audiobook: Audiobook,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cover-Bild oder Placeholder
                if (audiobook.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(audiobook.coverUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audiobook.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )

                    audiobook.author?.let { author ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fortschrittsbalken
            LinearProgressIndicator(
                progress = { audiobook.progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${(audiobook.progress * 100).toInt()}% abgeschlossen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Hilfsfunktion: Dauer formatieren
private fun formatDuration(milliseconds: Long): String {
    val hours = milliseconds / 3600000
    val minutes = (milliseconds % 3600000) / 60000
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        else -> "${minutes}min"
    }
}