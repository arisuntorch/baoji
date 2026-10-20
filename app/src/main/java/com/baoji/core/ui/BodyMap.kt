package com.baoji.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.Baoji
import com.baoji.domain.Muscle

/** Figures grow with the card but stop here so the list below stays in view. */
private val MaxFigureHeight = 180.dp

/** Share of each half of the card a figure's box may take, leaving air between the two. */
private const val HALF_FILL = 0.9f

/** Lightest red used for a barely worked muscle; the hardest-worked one gets full red. */
private const val MIN_RED = 0.3f

private val MUSCLE_NAMES = mapOf(
    Muscle.TRAPS to "斜方肌", Muscle.SHOULDERS to "肩", Muscle.CHEST to "胸", Muscle.BICEPS to "二头",
    Muscle.TRICEPS to "三头", Muscle.FOREARMS to "前臂", Muscle.ABS to "腹", Muscle.OBLIQUES to "腹斜肌",
    Muscle.LATS to "背阔肌", Muscle.LOWER_BACK to "下背", Muscle.GLUTES to "臀", Muscle.QUADS to "股四头",
    Muscle.ADDUCTORS to "内收肌", Muscle.HAMSTRINGS to "腘绳肌", Muscle.CALVES to "小腿",
)

/**
 * Front and back figures, gym-machine-sticker style: grey body, muscles worked today in red,
 * deeper red for harder work. [levels] come from [com.baoji.domain.MuscleMap.levels].
 */
@Composable
fun BodyMap(levels: Map<Muscle, Float>, modifier: Modifier = Modifier) {
    val worked = levels.entries.sortedByDescending { it.value }.mapNotNull { MUSCLE_NAMES[it.key] }
    BoxWithConstraints(
        modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = if (worked.isEmpty()) "人体图" else "今天练到：${worked.joinToString("、")}"
        },
    ) {
        val height = minOf(maxWidth / 2 * HALF_FILL / (BodyShapes.WIDTH / BodyShapes.HEIGHT), MaxFigureHeight)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Figure("正面", BodyShapes.FRONT, levels, absGaps = true, height = height)
            Figure("背面", BodyShapes.BACK, levels, absGaps = false, height = height)
        }
    }
}

@Composable
private fun Figure(label: String, muscles: List<Pair<Muscle, FloatArray>>, levels: Map<Muscle, Float>, absGaps: Boolean, height: Dp) {
    val c = Baoji.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(
            Modifier
                .size(width = height * (BodyShapes.WIDTH / BodyShapes.HEIGHT), height = height)
                .drawWithCache {
                    val scale = size.height / BodyShapes.HEIGHT
                    val body = smoothPath(withMirror(BodyShapes.SILHOUETTE), scale)
                    val head = BodyShapes.HEAD
                    val shapes = muscles.flatMap { (m, pts) ->
                        listOf(m to smoothPath(pts, scale), m to smoothPath(mirror(pts), scale))
                    }
                    val gaps = if (absGaps) BodyShapes.ABS_GAPS.flatMap { listOf(it, mirror(it)) } else emptyList()
                    onDrawBehind {
                        drawPath(body, c.fill)
                        drawOval(
                            c.fill,
                            topLeft = Offset((head[0] - head[2]) * scale, (head[1] - head[3]) * scale),
                            size = Size(head[2] * 2 * scale, head[3] * 2 * scale),
                        )
                        shapes.forEach { (m, path) -> drawPath(path, muscleColor(levels[m], c.card, c.muscle, c.muscleIdle)) }
                        gaps.forEach { g ->
                            drawLine(
                                c.fill, Offset(g[0] * scale, g[1] * scale), Offset(g[2] * scale, g[3] * scale),
                                strokeWidth = 0.9f * scale, cap = StrokeCap.Round,
                            )
                        }
                    }
                },
        )
        Spacer(Modifier.height(8.dp).width(1.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.secondaryText)
    }
}

private fun muscleColor(level: Float?, card: Color, red: Color, idle: Color): Color =
    if (level == null || level <= 0f) idle else lerp(card, red, MIN_RED + (1 - MIN_RED) * level.coerceAtMost(1f))

private fun mirror(pts: FloatArray): FloatArray =
    FloatArray(pts.size) { i -> if (i % 2 == 0) BodyShapes.WIDTH - pts[i] else pts[i] }

/** Left half followed by its mirror image walked backwards, sharing the centre-line end points. */
private fun withMirror(left: FloatArray): FloatArray {
    val n = left.size / 2
    val right = (n - 2 downTo 1).flatMap { i -> listOf(BodyShapes.WIDTH - left[2 * i], left[2 * i + 1]) }
    return left + right.toFloatArray()
}

/** Closed Catmull-Rom curve through the points, as cubic Béziers. */
private fun smoothPath(pts: FloatArray, scale: Float): Path {
    val n = pts.size / 2
    fun x(i: Int) = pts[2 * Math.floorMod(i, n)] * scale
    fun y(i: Int) = pts[2 * Math.floorMod(i, n) + 1] * scale
    return Path().apply {
        moveTo(x(0), y(0))
        for (i in 0 until n) {
            cubicTo(
                x(i) + (x(i + 1) - x(i - 1)) / 6, y(i) + (y(i + 1) - y(i - 1)) / 6,
                x(i + 1) - (x(i + 2) - x(i)) / 6, y(i + 1) - (y(i + 2) - y(i)) / 6,
                x(i + 1), y(i + 1),
            )
        }
        close()
    }
}
