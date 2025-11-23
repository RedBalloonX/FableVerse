package io.github.redballoonx.fableverse.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSelectFolderClick: () -> Unit
) {
    val selectedFolder by viewModel.selectedFolderUri.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Einstellungen",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hörbuch-Ordner Section
        Text(
            "Hörbuch-Ordner",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedFolder != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Aktueller Ordner:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        selectedFolder ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onSelectFolderClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning
        ) {
            Text(if (selectedFolder == null) "Ordner auswählen" else "Ordner ändern")
        }

        // Scan-Status
        if (isScanning) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Scanne Ordner...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Scan-Ergebnis
        scanResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            result,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = { viewModel.clearScanResult() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Schließen"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info-Text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ordnerstruktur",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    """
                    Erwartete Struktur:
                    
                    📁 Hörbücher/
                    ├─ 📁 Autor 1/
                    │  ├─ 📁 Buch 1/
                    │  │  ├─ 🎵 kapitel1.mp3
                    │  │  └─ 🎵 kapitel2.mp3
                    │  └─ 📁 Buch 2/
                    │     └─ 🎵 teil1.mp3
                    └─ 📁 Autor 2/
                       └─ 📁 Buch 3/
                          └─ 🎵 kapitel1.mp3
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}