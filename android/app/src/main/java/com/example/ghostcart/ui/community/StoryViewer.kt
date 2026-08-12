package com.example.ghostcart.ui.community

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.ghostcart.data.Analytics
import com.example.ghostcart.data.ContentBlockItem
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val STORY_DURATION_MS = 7000
private const val DISMISS_SWIPE_THRESHOLD_PX = 140f

/**
 * Full-screen story viewer (WhatsApp Status / Instagram/Facebook Stories
 * pattern): tap the left/right thirds to go back/forward, press-and-hold to
 * pause, pinch with two fingers to zoom (snaps back to the original position
 * the moment you let go - it never stays zoomed in), swipe down to close,
 * swipe up to reveal Like/Share.
 *
 * One hand-rolled gesture loop (not several stacked `pointerInput` gesture
 * detectors) so tap/drag/pinch don't fight over the same touch stream - a
 * single-finger gesture is classified as tap vs. vertical drag by movement
 * once it ends; a second finger touching down mid-gesture switches it to
 * pinch-zoom.
 *
 * A video story ([ContentBlockItem.mediaType] == "video") plays in full via
 * ExoPlayer instead of the fixed 7s image timer - it advances to the next
 * story when playback actually ends, not on a clock.
 *
 * Like is a local, per-session toggle only - there's no backend concept of
 * liking a content block yet, so nothing is persisted or counted server-side.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun StoryViewer(
    stories: List<ContentBlockItem>,
    startIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stories.isEmpty()) return
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, stories.lastIndex)) }
    var paused by remember { mutableStateOf(false) }
    var progress by remember(index) { mutableStateOf(0f) }
    var scale by remember(index) { mutableStateOf(1f) }
    var offsetX by remember(index) { mutableStateOf(0f) }
    var offsetY by remember(index) { mutableStateOf(0f) }
    var showActions by remember { mutableStateOf(false) }
    var likedIndices by remember { mutableStateOf(setOf<Int>()) }

    val animatedScale by animateFloatAsState(scale, animationSpec = tween(220), label = "storyScale")
    val animatedOffsetX by animateFloatAsState(offsetX, animationSpec = tween(220), label = "storyOffsetX")
    val animatedOffsetY by animateFloatAsState(offsetY, animationSpec = tween(220), label = "storyOffsetY")

    val currentStory = stories[index]
    val exoPlayer = remember(index) {
        if (currentStory.mediaType == "video") {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(currentStory.imageUrl))
                prepare()
            }
        } else null
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }
    LaunchedEffect(paused, exoPlayer) {
        exoPlayer?.playWhenReady = !paused
    }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun goNext() {
        if (index < stories.lastIndex) index += 1 else onClose()
    }
    fun goPrevious() {
        if (index > 0) index -= 1
    }

    LaunchedEffect(index) {
        Analytics.logStoryViewed(context, index)
    }

    LaunchedEffect(index, paused, showActions, exoPlayer) {
        if (paused || showActions) return@LaunchedEffect
        if (exoPlayer != null) {
            while (true) {
                delay(100L)
                val duration = exoPlayer.duration
                if (duration > 0) {
                    progress = (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                    goNext()
                    break
                }
            }
            return@LaunchedEffect
        }
        val tickMs = 16L
        while (progress < 1f) {
            delay(tickMs)
            if (!paused && !showActions) progress = (progress + tickMs.toFloat() / STORY_DURATION_MS).coerceAtMost(1f)
        }
        if (progress >= 1f) goNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val story = currentStory
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(index) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        paused = true
                        val startTime = System.currentTimeMillis()
                        var totalDx = 0f
                        var totalDy = 0f
                        var pinched = false
                        var lastDistance = 0f
                        var lastCentroid = down.position

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break

                            if (pressed.size >= 2) {
                                pinched = true
                                val p1 = pressed[0].position
                                val p2 = pressed[1].position
                                val centroid = androidx.compose.ui.geometry.Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                                val distance = kotlin.math.hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()).toFloat()
                                if (lastDistance > 0f) {
                                    val zoomDelta = distance / lastDistance
                                    scale = (scale * zoomDelta).coerceIn(1f, 4f)
                                    offsetX += centroid.x - lastCentroid.x
                                    offsetY += centroid.y - lastCentroid.y
                                }
                                lastCentroid = centroid
                                lastDistance = distance
                                event.changes.forEach { it.consume() }
                            } else {
                                val change = pressed[0]
                                val delta = change.positionChange()
                                totalDx += delta.x
                                totalDy += delta.y
                                if (scale > 1f) {
                                    offsetX += delta.x
                                    offsetY += delta.y
                                    change.consume()
                                }
                            }
                        }

                        paused = false
                        val elapsed = System.currentTimeMillis() - startTime

                        when {
                            pinched -> resetZoom()
                            scale > 1f -> Unit
                            totalDy > DISMISS_SWIPE_THRESHOLD_PX && abs(totalDy) > abs(totalDx) -> onClose()
                            totalDy < -DISMISS_SWIPE_THRESHOLD_PX && abs(totalDy) > abs(totalDx) -> showActions = true
                            elapsed < 250 && abs(totalDx) < 24 && abs(totalDy) < 24 -> {
                                if (down.position.x < size.width / 3f) goPrevious() else goNext()
                            }
                        }
                    }
                }
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffsetX,
                            translationY = animatedOffsetY
                        )
                )
            } else {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffsetX,
                            translationY = animatedOffsetY
                        )
                )
            }
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

        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    val liked = index in likedIndices
                    StoryActionButton(
                        icon = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        tint = if (liked) Color(0xFFFF4D67) else Color.White,
                        label = "Like",
                        onClick = {
                            likedIndices = if (liked) likedIndices - index else likedIndices + index
                        }
                    )
                    StoryActionButton(
                        icon = Icons.Filled.Share,
                        tint = Color.White,
                        label = "Share",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Ghost Cart Story")
                                putExtra(Intent.EXTRA_TEXT, "Check this out on Ghost Cart:\n${story.imageUrl}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share this story"))
                            Analytics.logShare(context, "story")
                        }
                    )
                }
                Text(
                    text = "Tap to close",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 14.dp).clickable { showActions = false }
                )
            }
        }
    }
}

@Composable
private fun StoryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}
