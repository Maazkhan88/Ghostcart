package com.example.ghostcart.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Central source of animation specs for the app's M3E motion. Every custom
 * animation introduced for the nav bar / chips / segmented control should
 * pull its spec from here instead of inlining spring(...) tuples, so (a)
 * everything stays derived from the real MaterialTheme.motionScheme set in
 * GhostCartTheme, and (b) reduced-motion support only needs to live once.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object GhostMotion {

    @Composable
    fun colorSpec(): FiniteAnimationSpec<Color> =
        if (reduceMotion()) snap() else MaterialTheme.motionScheme.defaultEffectsSpec()

    @Composable
    fun popSpec(): FiniteAnimationSpec<Float> =
        if (reduceMotion()) snap() else MaterialTheme.motionScheme.fastSpatialSpec()

    @Composable
    fun sizeSpec(): FiniteAnimationSpec<Dp> =
        if (reduceMotion()) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()

    @Composable
    fun offsetSpec(): FiniteAnimationSpec<IntOffset> =
        if (reduceMotion()) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()

    @Composable
    fun morphSpec(): FiniteAnimationSpec<Float> =
        if (reduceMotion()) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()

    @Composable
    fun reduceMotion(): Boolean = rememberReduceMotionEnabled()
}

/** Named shape presets for shape-morph, kept in one place so the exact look is a one-line swap. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object GhostMorphShapes {
    val circle: RoundedPolygon get() = MaterialShapes.Circle
    val blob: RoundedPolygon get() = MaterialShapes.Cookie9Sided
}

@Composable
fun rememberGhostMorph(start: RoundedPolygon, end: RoundedPolygon): Morph =
    remember(start, end) { Morph(start, end) }

/**
 * RoundedPolygon/Morph coordinates are normalized around the origin with a
 * radius of roughly 1, so every consumer needs the same scale+translate into
 * the actual layout bounds. Centralized here so call sites only ever pass a
 * plain 0f..1f progress.
 */
private class GhostMorphShape(
    private val morph: Morph,
    private val progress: () -> Float,
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        matrix.reset()
        val path = morph.toPath(progress().coerceIn(0f, 1f)).asComposePath()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/** Clips content to a shape that morphs between [morph]'s start and end polygons as [progress] moves 0f..1f. */
fun Modifier.ghostMorphClip(morph: Morph, progress: () -> Float): Modifier =
    this.clip(GhostMorphShape(morph, progress))

/**
 * Tracks Android's system "Remove animations" accessibility setting
 * (Settings.Global.ANIMATOR_DURATION_SCALE == 0f), live-updating via a
 * ContentObserver so a mid-session toggle is respected immediately.
 */
@Composable
fun rememberReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    var reduceMotion by remember { mutableStateOf(isAnimatorDurationScaleZero(context)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = isAnimatorDurationScaleZero(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduceMotion
}

private fun isAnimatorDurationScaleZero(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
