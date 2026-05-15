package com.swipecleaner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipecleaner.app.data.ThemeMode
import com.swipecleaner.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by vm.state.collectAsState()
    var showResetKeptDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }
    var albumMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadAlbums() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionTitle("Apariencia")
            ThemeMode.values().forEach { mode ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.prefs.themeMode == mode,
                        onClick = { vm.setTheme(mode) }
                    )
                    Text(
                        when (mode) {
                            ThemeMode.SYSTEM -> "Seguir el sistema"
                            ThemeMode.LIGHT -> "Claro"
                            ThemeMode.DARK -> "Oscuro"
                        }
                    )
                }
            }

            Divider()

            SectionTitle("Reaparición de fotos conservadas")
            val months = (state.prefs.reappearAfterMillis / (30L * 24L * 60L * 60L * 1000L)).toInt()
                .coerceIn(1, 24)
            Text(
                "Las fotos marcadas como 'conservar' volverán a aparecer pasados $months meses.",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = months.toFloat(),
                onValueChange = { value -> vm.setReappearMonths(value.toInt()) },
                valueRange = 1f..24f,
                steps = 22
            )

            Divider()

            SectionTitle("Álbum/Carpeta")
            val currentAlbumName = state.albums.firstOrNull { album ->
                album.bucketId == state.prefs.selectedBucketId
            }?.name ?: "Todos los álbumes"
            OutlinedButton(
                onClick = { albumMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(currentAlbumName)
            }
            DropdownMenu(
                expanded = albumMenuExpanded,
                onDismissRequest = { albumMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todos los álbumes") },
                    onClick = {
                        vm.setBucket(null)
                        albumMenuExpanded = false
                    }
                )
                state.albums.forEach { album ->
                    DropdownMenuItem(
                        text = { Text(album.name + " (" + album.count + ")") },
                        onClick = {
                            vm.setBucket(album.bucketId)
                            albumMenuExpanded = false
                        }
                    )
                }
            }

            Divider()

            SectionTitle("Reiniciar")
            Button(
                onClick = { showResetKeptDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) { Text("Reiniciar lista de conservadas") }

            Button(
                onClick = { showResetAllDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Borrar TODO el historial") }

            Spacer(Modifier.height(40.dp))
            Text(
                "SwipeCleaner v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showResetKeptDialog) {
        ConfirmDialog(
            title = "Reiniciar conservadas",
            text = "Todas las fotos marcadas como 'conservar' volverán a aparecer. ¿Continuar?",
            onConfirm = {
                vm.resetKeptList()
                showResetKeptDialog = false
            },
            onDismiss = { showResetKeptDialog = false }
        )
    }
    if (showResetAllDialog) {
        ConfirmDialog(
            title = "Borrar historial",
            text = "Se borrarán todos los registros (conservadas y papelera lógica). " +
                    "Las fotos ya eliminadas físicamente no se recuperan. ¿Continuar?",
            onConfirm = {
                vm.resetAll()
                showResetAllDialog = false
            },
            onDismiss = { showResetAllDialog = false }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Sí, continuar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
