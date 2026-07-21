package com.example.ghostcart.ui.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.ContentBlockItem
import kotlinx.coroutines.delay

private const val STORY_DURATION_MS = 3000

/**
 * Full-screen story viewer (WhatsApp Status / Instagram/Facebook Stories
 * pattern): each story auto-advances after [STORY_DURATION_MS], tapping the
 * left/right thirds of the screen goes back/forward, a press-and-hold pauses
 * the timer, and the current image supports pinch-to-zoom + pan while held.
 *
 * Video isn't supported yet - the backend's content-blocks upload pipeline
 * (lib/image-processing.ts) only accepts PNG/JPEG today, so every
 * [ContentBlockItem] is an image. This is written so a future `isVideo`
 * flag could branch to a player without restructuring the viewer, but no
 * video playback is wired up since there's no video content to play.
 */
@Composable
fun StoryViewer(
    stories: List<ContentBlockItem>,
    startIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stories.isEmpty()) return
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, stories.lastIndex)) }
    var paused by remember { mutableStateOf(false) }
    var progress by remember(index) { mutableFloatStateOf(0f) }
    var scale by remember(index) { mutableFloatStateOf(1f) }
    var offsetX by remember(index) { mutableFloatStateOf(0f) }
    var offsetY by remember(index) { mutableFloatStateOf(0f) }

    fun goNext() {
        if (index < stories.lastIndex) index += 1 else onClose()
    }
    fun goPrevious() {
        if (index > 0) index -= 1
    }

    LaunchedEffect(index, paused) {
        if (paused) return@LaunchedEffect
        val tickMs = 16L
        while (progress < 1f) {
            delay(tickMs)
            if (!paused) progress = (progress + tickMs.toFloat() / STORY_DURATION_MS).coerceAtMost(1f)
        }
        if (progress >= 1f) goNext()
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val story = stories[index]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(index) {
                    detectTapGestures(
                        onPress = {
                            paused = true
                            tryAwaitRelease()
                            paused = false
                        },
                        onTap = { tapOffset ->
                            if (tapOffset.x < size.width / 3f) goPrevious() else goNext()
                        }
                    )
                }
        ) {
            AsyncImage(
                model = story.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .transformable(transformState)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            stories.forEachIndexed { i, _ ->
                val segmentProgress = when {
                    i < index -> 1f
                    i == index -> progress
                    else -> 0f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(segmentProgress)
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 24.dp, end = 4.dp)
                .size(40.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
