package com.swipecleaner.app.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val state = rememberMultiplePermissionsState(permissions = permissions)

    if (state.allPermissionsGranted) {
        content()
    } else {
        PermissionRequestScreen(state)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestScreen(state: MultiplePermissionsState) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            null,
            modifier = Modifier.height(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Necesitamos acceso a tus fotos",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "SwipeCleaner usa el permiso solo para mostrarte tus fotos en pantalla " +
                    "y mover a una papelera interna las que decidas eliminar. " +
                    "Nada sale de tu dispositivo.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { state.launchMultiplePermissionRequest() }) {
            Text("Conceder permiso")
        }
    }
}
