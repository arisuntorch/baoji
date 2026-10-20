package com.baoji.feature.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.PageHeader
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.SolidButton
import com.baoji.core.ui.baojiFieldColors
import com.baoji.core.ui.trimmed
import com.baoji.domain.CurvePoint
import java.time.LocalDate

@Composable
fun WeightScreen(onBack: () -> Unit, vm: WeightViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val chart by vm.chart.collectAsStateWithLifecycle()
    val input by vm.input.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        PageHeader("体重", onBack)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = vm::setInput,
                label = { Text("今天 kg", color = c.secondaryText) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.save() }),
                colors = baojiFieldColors(),
                modifier = Modifier.weight(1f),
            )
            SolidButton("记录", enabled = input.isNotBlank(), onClick = { vm.save() })
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.primaryText,
                modifier = Modifier.padding(horizontal = PagePadding, vertical = 4.dp))
        }
        Spacer(Modifier.height(24.dp))
        if (!chart.isEmpty) {
            WeightCurveChart(chart, Modifier.fillMaxWidth().weight(1f).padding(horizontal = PagePadding, vertical = 16.dp))
        }
    }
}

/** X-only viewport over epoch days. Starts showing everything; pinch zooms, drag pans. */
private data class Viewport(val from: Double, val to: Double) {
    val span: Double get() = to - from
}

@Composable
private fun WeightCurveChart(chart: WeightChart, modifier: Modifier) {
    val c = Baoji.colors
    val dataMin = chart.raw.first().x
    val dataMax = chart.raw.last().x
    val full = Viewport(dataMin - 0.5, dataMax + 0.5)
    var view by remember(dataMin, dataMax) { mutableStateOf(full) }
    val minSpan = 3.0

    val latest = chart.raw.last()
    val desc = "体重曲线，共 ${chart.raw.size} 条记录，最近 ${LocalDate.ofEpochDay(latest.x.toLong())} ${latest.y.trimmed()} kg"

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { contentDescription = desc }
                .pointerInput(full) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val v = view
                        val w = size.width.toDouble()
                        val anchor = v.from + centroid.x / w * v.span
                        val newSpan = (v.span / zoom).coerceIn(minSpan, full.span)
                        var from = anchor - (anchor - v.from) * (newSpan / v.span)
                        from -= pan.x / w * newSpan
                        from = from.coerceIn(full.from, full.to - newSpan)
                        view = Viewport(from, from + newSpan)
                    }
                },
        ) {
            val visibleRaw = chart.raw.filter { it.x in view.from..view.to }.ifEmpty { chart.raw }
            val visibleSmooth = chart.smooth.filter { it.x in view.from - 1..view.to + 1 }
            val ys = visibleRaw.map { it.y } + visibleSmooth.map { it.y }
            val yMin = ys.min() - 0.5
            val yMax = ys.max() + 0.5
            fun px(p: CurvePoint) = Offset(
                ((p.x - view.from) / view.span * size.width).toFloat(),
                ((1 - (p.y - yMin) / (yMax - yMin)) * size.height).toFloat(),
            )
            visibleRaw.forEach { drawCircle(c.secondaryText.copy(alpha = 0.45f), 2.5.dp.toPx(), px(it)) }
            if (visibleSmooth.size >= 2) {
                val path = Path()
                visibleSmooth.forEachIndexed { i, p -> px(p).let { o -> if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) } }
                drawPath(path, c.primaryText, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val a = LocalDate.ofEpochDay(Math.ceil(view.from).toLong())
            val b = LocalDate.ofEpochDay(Math.floor(view.to).toLong())
            Text("${a.monthValue}/${a.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = c.secondaryText)
            Text("${b.monthValue}/${b.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = c.secondaryText)
        }
    }
}
