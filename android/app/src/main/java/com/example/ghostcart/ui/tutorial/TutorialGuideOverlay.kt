package com.example.ghostcart.ui.tutorial

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.Paper
import com.ghostcart.app.R
import kotlin.math.roundToInt

data class TutorialGuideSpec(
    val message: String,
    val stepLabel: String,
    val teacherImageRes: Int = R.drawable.tutorial_teacher_pointer
)

/**
 * In-context tutorial spotlight. Production UI remains mounted underneath and the
 * highlighted production control remains the only tappable hole in the overlay.
 */
@Composable
fun TutorialGuideOverlay(
    targetBounds: Rect?,
    guide: TutorialGuideSpec,
    modifier: Modifier = Modifier
) {
    if (targetBounds == null) return
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val paddingPx = with(density) { 9.dp.toPx() }
        val target = Rect(
            left = (targetBounds.left - paddingPx).coerceIn(0f, widthPx),
            top = (targetBounds.top - paddingPx).coerceIn(0f, heightPx),
            right = (targetBounds.right + paddingPx).coerceIn(0f, widthPx),
            bottom = (targetBounds.bottom + paddingPx).coerceIn(0f, heightPx)
        )

        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val scrim = Color.Black.copy(alpha = 0.70f)
            drawRect(scrim, size = androidx.compose.ui.geometry.Size(size.width, target.top))
            drawRect(
                scrim,
                topLeft = androidx.compose.ui.geometry.Offset(0f, target.bottom),
                size = androidx.compose.ui.geometry.Size(size.width, (size.height - target.bottom).coerceAtLeast(0f))
            )
            drawRect(
                scrim,
                topLeft = androidx.compose.ui.geometry.Offset(0f, target.top),
                size = androidx.compose.ui.geometry.Size(target.left, target.height)
            )
            drawRect(
                scrim,
                topLeft = androidx.compose.ui.geometry.Offset(target.right, target.top),
                size = androidx.compose.ui.geometry.Size((size.width - target.right).coerceAtLeast(0f), target.height)
            )
            drawRoundRect(
                color = GhostGreen,
                topLeft = androidx.compose.ui.geometry.Offset(target.left, target.top),
                size = androidx.compose.ui.geometry.Size(target.width, target.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        TutorialTouchBlocker(0f, 0f, widthPx, target.top, density.density, interactionSource)
        TutorialTouchBlocker(0f, target.bottom, widthPx, heightPx - target.bottom, density.density, interactionSource)
        TutorialTouchBlocker(0f, target.top, target.left, target.height, density.density, interactionSource)
        TutorialTouchBlocker(target.right, target.top, widthPx - target.right, target.height, density.density, interactionSource)

        Surface(
            color = Color.Black,
            contentColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, FaintBorder),
            modifier = Modifier
                .align(if (target.center.y > heightPx * 0.55f) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(interactionSource = interactionSource, indication = null) { }
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                FloatingTutorialTeacher(imageRes = guide.teacherImageRes)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = guide.stepLabel,
                        color = GhostGreen,
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = guide.message,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingTutorialTeacher(
    modifier: Modifier = Modifier,
    imageRes: Int = R.drawable.tutorial_teacher_pointer,
    contentDescription: String = "Ghost Cart tutorial guide"
) {
    val transition = rememberInfiniteTransition(label = "tutorial_teacher_float")
    val offsetY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "tutorial_teacher_y"
    )
    val rotation by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "tutorial_teacher_rotation"
    )
    Image(
        painter = painterResource(imageRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .graphicsLayer {
                translationY = offsetY
                rotationZ = rotation
            }
    )
}

/**
 * Compatibility entry point used by the existing cooldown picker integration.
 * It now renders the supplied tutorial teacher instead of the generic mascot.
 */
@Composable
fun FloatingTutorialMascot(modifier: Modifier = Modifier) {
    FloatingTutorialTeacher(modifier = modifier)
}

@Composable
private fun TutorialTouchBlocker(
    xPx: Float,
    yPx: Float,
    widthPx: Float,
    heightPx: Float,
    density: Float,
    interactionSource: MutableInteractionSource
) {
    if (widthPx <= 0f || heightPx <= 0f) return
    Box(
        Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size((widthPx / density).dp, (heightPx / density).dp)
            .background(Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { }
    )
}
