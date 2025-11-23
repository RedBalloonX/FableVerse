package io.github.redballoonx.fableverse.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.redballoonx.fableverse.data.model.Audiobook
import io.github.redballoonx.fableverse.player.PlayerState
import java.io.File
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    audiobookId: Long,
    onBack: () -> Unit
) {
    val audiobook by viewModel.audiobook.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()

    var showChapterList by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Lade Hörbuch beim Start
    LaunchedEffect(audiobookId) {
        viewModel.loadAudiobook(audiobookId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wiedergabe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    // Sleep Timer Button
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Badge(
                            containerColor = if (sleepTimerMinutes != null)
                                MaterialTheme.colorScheme.error
                            else Color.Transparent
                        ) {
                            Icon(Icons.Default.Timer, "Sleep Timer")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        audiobook?.let { book ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    if (book.coverUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(book.coverUri))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Titel & Autor
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                book.author?.let { author ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = author,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Kapitel-Info mit Dropdown
                TextButton(onClick = { showChapterList = !showChapterList }) {
                    Text("Kapitel ${playerState.currentChapterIndex + 1} / ${book.chapters.size}")
                    Icon(
                        if (showChapterList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                // Kapitel-Liste (expandable)
                if (showChapterList) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        LazyColumn {
                            itemsIndexed(book.chapters) { index, chapter ->
                                ListItem(
                                    headlineContent = { Text(chapter.title) },
                                    leadingContent = {
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    trailingContent = {
                                        if (index == playerState.currentChapterIndex) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        viewModel.jumpToChapter(index)
                                        showChapterList = false
                                    }
                                )
                                if (index < book.chapters.size - 1) {
                                    Divider()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Fortschrittsbalken
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = playerState.position.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..(playerState.duration.toFloat().coerceAtLeast(1f)),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(playerState.position),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            formatTime(playerState.duration),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback-Speed Button
                TextButton(onClick = { showSpeedDialog = true }) {
                    Icon(Icons.Default.Speed, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${playerState.playbackSpeed}x")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vorheriges Kapitel
                    IconButton(
                        onClick = { viewModel.previousChapter() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Vorheriges Kapitel",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // 30s zurück
                    IconButton(
                        onClick = {
                            viewModel.seekTo((playerState.position - 30000).coerceAtLeast(0))
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = "30s zurück",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(72.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Abspielen",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 30s vorwärts
                    IconButton(
                        onClick = {
                            viewModel.seekTo(
                                (playerState.position + 30000).coerceAtMost(playerState.duration)
                            )
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward,
                            contentDescription = "30s vorwärts",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Nächstes Kapitel
                    IconButton(
                        onClick = { viewModel.nextChapter() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Nächstes Kapitel",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Geschwindigkeits-Dialog
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = playerState.playbackSpeed,
            onSpeedSelected = { speed ->
                viewModel.setPlaybackSpeed(speed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    // Sleep-Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            onTimerSet = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wiedergabegeschwindigkeit") },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${speed}x")
                        if (speed == currentSpeed) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onTimerSet: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val timerOptions = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                // Timer deaktivieren
                if (currentMinutes != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimerSet(null) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Timer deaktivieren")
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Divider()
                }

                // Timer-Optionen
                timerOptions.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimerSet(minutes) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$minutes Minuten")
                        if (currentMinutes == minutes) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}