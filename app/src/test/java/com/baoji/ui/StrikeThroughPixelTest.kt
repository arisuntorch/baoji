package com.baoji.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.BaojiTheme
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.repo.ExerciseRow
import com.baoji.feature.training.ExerciseRowItem
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A finished exercise must show a thin line through its name (plan §训练: 名字上划一条细线). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class StrikeThroughPixelTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var done by mutableStateOf(0)

    private fun show(name: String, initial: Int) {
        done = initial
        val exercise = ExerciseEntity("e1", "p1", name, 3, 5, 8, 10.0, 80.0, 5, 0, false, 0)
        rule.setContent {
            BaojiTheme(dark = true) {
                Box(Modifier.background(Color.Black).padding(horizontal = 16.dp)) {
                    ExerciseRowItem(ExerciseRow(exercise, done, overload = false), {}, {}, {})
                }
            }
        }
    }

    /** Renders the window and reports, for each column in [from, to), whether the name's middle row is lit. */
    private fun middleRowLit(name: String, from: (Rect) -> Int, to: (Rect) -> Int): List<Boolean> {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        val root = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val b = rule.onNodeWithText(name).fetchSemanticsNode().boundsInWindow
        val y = ((b.top + b.bottom) / 2).toInt()
        return (from(b) until to(b)).map { x ->
            (y - 1..y + 1).any { yy -> (bitmap.getPixel(x, yy) shr 16 and 0xFF) > 40 }
        }
    }

    private fun litAcrossName(name: String) =
        middleRowLit(name, { it.left.toInt() }, { it.right.toInt() }).count { it }

    private fun nameWidth(name: String) = rule.onNodeWithText(name).fetchSemanticsNode().size.width

    @Test
    fun completedNameIsStruckThrough() {
        show(GAPPY, initial = 3)
        // A full-width line lights (almost) every column; the glyphs alone leave wide gaps.
        assertThat(litAcrossName(GAPPY)).isAtLeast((nameWidth(GAPPY) * 0.9).toInt())
    }

    @Test
    fun unfinishedNameHasNoLine() {
        show(GAPPY, initial = 1)
        assertThat(litAcrossName(GAPPY)).isLessThan((nameWidth(GAPPY) * 0.9).toInt())
    }

    @Test
    fun lineAppearsWhenLastSetIsDone() {
        show(GAPPY, initial = 2)
        litAcrossName(GAPPY)
        done = 3
        assertThat(litAcrossName(GAPPY)).isAtLeast((nameWidth(GAPPY) * 0.9).toInt())
    }

    @Test
    fun lineOverhangsCjkNameSoItStaysVisible() {
        // 深蹲 has a horizontal stroke at mid-height; the line must stick out past the glyphs to read as a strike.
        show("深蹲", initial = 3)
        val pastRightEdge = middleRowLit("深蹲", { it.right.toInt() + 1 }, { it.right.toInt() + 4 })
        assertThat(pastRightEdge).doesNotContain(false)
    }

    @Test
    fun unfinishedCjkNameHasNothingPastItsEdge() {
        show("深蹲", initial = 1)
        val pastRightEdge = middleRowLit("深蹲", { it.right.toInt() + 1 }, { it.right.toInt() + 4 })
        assertThat(pastRightEdge).doesNotContain(true)
    }

    private companion object {
        // Wide gaps between thin glyphs: only a strike line can light most columns of the middle row.
        const val GAPPY = "i      i"
    }
}
