package com.swipecleaner.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.swipecleaner.app.data.MediaPhoto
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val SWIPE_THRESHOLD_DP = 120

@Composable
fun SwipeableCard(
    photo: MediaPhoto,
    onSwipeLeft: () -> Unit, // TRASH
    onSwipeRight: () -> Unit, // KEEP
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Reset cuando cambia la foto
    LaunchedEffect(photo.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    val rotation = (offsetX.value / 40f).coerceIn(-15f, 15f)
    val alpha = 1f - (offsetX.value.absoluteValue / 1000f).coerceIn(0f, 0.3f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                translationX = offsetX.value,
                translationY = offsetY.value,
                rotationZ = rotation,
                alpha = alpha
            )
            .pointerInput(photo.id) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > thresholdPx -> {
                                    offsetX.animateTo(2000f, tween(280))
                                    onSwipeRight()
                                }
                                offsetX.value < -thresholdPx -> {
                                    offsetX.animateTo(-2000f, tween(280))
                                    onSwipeLeft()
                                }
                                else -> {
                                    offsetX.animateTo(0f, tween(200))
                                    offsetY.animateTo(0f, tween(200))
                                }
                            }
                        }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + drag.x)
                            offsetY.snapTo(offsetY.value + drag.y * 0.3f)
                        }
                    }
                )
            }
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Etiqueta visual sobre la imagen según dirección de arrastre
        if (offsetX.value > 40f) {
            ActionLabel(
                text = "CONSERVAR",
                color = Color(0xFF2E7D32),
                alignment = Alignment.TopStart,
                modifier = Modifier.padding(24.dp)
            )
        } else if (offsetX.value < -40f) {
            ActionLabel(
                text = "ELIMINAR",
                color = Color(0xFFD32F2F),
                alignment = Alignment.TopEnd,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
private fun ActionLabel(
    text: String,
    color: Color,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(alignment)
                .background(color, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = text, color = Color.White)
        }
    }
}
