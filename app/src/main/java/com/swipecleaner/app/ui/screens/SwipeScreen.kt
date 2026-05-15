package com.swipecleaner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipecleaner.app.ui.components.SwipeableCard
import com.swipecleaner.app.ui.viewmodel.SwipeViewModel
import com.swipecleaner.app.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val vm: SwipeViewModel = viewModel(factory = SwipeViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadPhotos() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SwipeCleaner", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Liberado: ${Formatters.bytesToHuman(state.freedBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Default.Delete, contentDescription = "Papelera")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.currentPhoto == null -> EmptyState(
                    message = state.emptyMessage ?: "No hay fotos disponibles",
                    onReload = vm::loadPhotos
                )
                else -> SwipeContent(
                    state = state,
                    onKeep = vm::keepCurrent,
                    onTrash = vm::trashCurrent,
                    onUndo = vm::undo
                )
            }
        }
    }
}

@Composable
private fun SwipeContent(
    state: com.swipecleaner.app.ui.viewmodel.SwipeUiState,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onUndo: () -> Unit
) {
    val current = state.currentPhoto ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Foto ${state.currentIndex + 1} de ${state.photos.size} · ${Formatters.bytesToHuman(state.currentPhoto?.sizeBytes ?: 0)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
            contentAlignment = Alignment.Center
        ) {
            // Foto siguiente como placeholder atrás
            state.nextPhoto?.let { next ->
                SwipeableCard(
                    photo = next,
                    onSwipeLeft = {},
                    onSwipeRight = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            SwipeableCard(
                photo = current,
                onSwipeLeft = onTrash,
                onSwipeRight = onKeep
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = current.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${Formatters.bytesToHuman(current.sizeBytes)} · ${current.bucketName ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = onTrash,
                    containerColor = Color(0xFFD32F2F)
                ) {
                    Icon(Icons.Default.Close, "Eliminar", tint = Color.White)
                }
                FloatingActionButton(
                    onClick = onUndo,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Undo, "Deshacer")
                }
                FloatingActionButton(
                    onClick = onKeep,
                    containerColor = Color(0xFF2E7D32)
                ) {
                    Icon(Icons.Default.Favorite, "Conservar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, onReload: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Toca para recargar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(8.dp)
        )
        FloatingActionButton(onClick = onReload) {
            Icon(Icons.Default.Undo, "Recargar")
        }
    }
}
