package com.baoji.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeLeft
import com.baoji.data.db.ActivityKind
import com.baoji.data.db.ActivityLogEntity
import com.baoji.feature.training.StepsRowItem
import com.baoji.core.theme.BaojiTheme
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.MealTaskEntity
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.repo.DietDay
import com.baoji.data.repo.ExerciseRow
import com.baoji.data.repo.Macros
import com.baoji.data.repo.MealRow
import com.baoji.domain.Sex
import com.baoji.feature.diet.DietContent
import com.baoji.feature.diet.MealActions
import com.baoji.feature.onboarding.OnboardingForm
import com.baoji.feature.onboarding.ProfileStep
import com.baoji.feature.training.ExerciseRowItem
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComponentsUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val bench = ExerciseEntity("e1", "p1", "卧推", 4, 6, 8, 8.0, 60.0, 8, 0, false, 0)

    @Test
    fun setButtonTapAddsAndLongPressResetsTheExercise() {
        rule.setContent {
            BaojiTheme(dark = false) {
                var done by remember { mutableStateOf(1) }
                ExerciseRowItem(
                    row = ExerciseRow(bench, done, overload = false),
                    onOpen = {},
                    onSet = { done++ },
                    onReset = { done = 0 },
                )
            }
        }
        rule.onNodeWithText("1/4").performClick()
        rule.onNodeWithText("2/4").performTouchInput { longClick() }
        rule.onNodeWithText("0/4").assertExists()
    }

    @Test
    fun overloadArrowShownOnlyWhenMarked() {
        rule.setContent {
            BaojiTheme(dark = false) {
                ExerciseRowItem(ExerciseRow(bench, 0, overload = true), {}, {}, {})
            }
        }
        rule.onNodeWithText("↑").assertExists()
    }

    @Test
    fun mealToggleChecksAndUnchecks() {
        val task = MealTaskEntity("m1", "练前鸡蛋", 2.0, "个", 12.0, 1.0, 10.0, 142.0, 0, false, 0)
        val toggles = mutableListOf<String>()
        rule.setContent {
            BaojiTheme(dark = false) {
                var checked by remember { mutableStateOf(false) }
                DietContent(
                    day = DietDay(LocalDate.of(2026, 10, 12), listOf(MealRow(task, checked)), emptyList(), Macros.ZERO, NutritionTargets.EMPTY),
                    snacks = emptyList(),
                    actions = MealActions(onToggle = { toggles += it; checked = !checked }),
                    onSnack = {},
                )
            }
        }
        rule.onNodeWithText("练前鸡蛋").performClick()
        rule.onNodeWithText("练前鸡蛋").performClick()
        assertThat(toggles).containsExactly("m1", "m1")
    }

    @Test
    fun mealLongPressResets() {
        val task = MealTaskEntity("m1", "练前鸡蛋", 2.0, "个", 12.0, 1.0, 10.0, 142.0, 0, false, 0)
        val resets = mutableListOf<String>()
        rule.setContent {
            BaojiTheme(dark = false) {
                DietContent(
                    day = DietDay(LocalDate.of(2026, 10, 12), listOf(MealRow(task, true)), emptyList(), Macros.ZERO, NutritionTargets.EMPTY),
                    snacks = emptyList(),
                    actions = MealActions(onToggle = {}, onReset = { resets += it }),
                    onSnack = {},
                )
            }
        }
        rule.onNodeWithText("练前鸡蛋").performTouchInput { longClick() }
        assertThat(resets).containsExactly("m1")
    }

    @Test
    fun mealSwipeEitherWayDeletes() {
        val egg = MealTaskEntity("m1", "练前鸡蛋", 2.0, "个", 12.0, 1.0, 10.0, 142.0, 0, false, 0)
        val oats = MealTaskEntity("m2", "燕麦", 50.0, "克", 6.0, 33.0, 3.0, 183.0, 1, false, 0)
        val archived = mutableListOf<String>()
        rule.setContent {
            BaojiTheme(dark = false) {
                DietContent(
                    day = DietDay(LocalDate.of(2026, 10, 12), listOf(MealRow(egg, false), MealRow(oats, false)), emptyList(), Macros.ZERO, NutritionTargets.EMPTY),
                    snacks = emptyList(),
                    actions = MealActions(onToggle = {}, onArchive = { archived += it }),
                    onSnack = {},
                )
            }
        }
        rule.onNodeWithText("删除").assertDoesNotExist()
        rule.onNodeWithText("练前鸡蛋").performTouchInput { swipeLeft() }
        rule.onNodeWithText("燕麦").performTouchInput { swipeRight() }
        rule.waitForIdle()
        assertThat(archived).containsExactly("m1", "m2")
    }

    @Test
    fun stepsRowShowsCountTapEditsLongPressResets() {
        val steps = ActivityLogEntity("steps-1", 1, 0, ActivityKind.STEPS, "每日步数", 8000.0, "步", 212.1, 0)
        var taps = 0
        var resets = 0
        rule.setContent {
            BaojiTheme(dark = false) { StepsRowItem(steps, onTap = { taps++ }, onReset = { resets++ }) }
        }
        rule.onNodeWithText("8,000 步 · 212 kcal").assertExists()
        rule.onNodeWithText("每日步数").performClick()
        rule.onNodeWithText("每日步数").performTouchInput { longClick() }
        assertThat(taps).isEqualTo(1)
        assertThat(resets).isEqualTo(1)
    }

    @Test
    fun onboardingContinueNeedsAllRequiredFields() {
        var form by mutableStateOf(OnboardingForm())
        rule.setContent {
            BaojiTheme(dark = false) {
                ProfileStep(
                    form = form,
                    onHeight = { form = form.copy(height = it) },
                    onWeight = { form = form.copy(weight = it) },
                    onAge = { form = form.copy(age = it) },
                    onSex = { form = form.copy(sex = it) },
                    onSubmit = {},
                )
            }
        }
        rule.onNodeWithTag("submit").assertIsNotEnabled()
        rule.onNodeWithTag("height").performTextInput("175")
        rule.onNodeWithTag("weight").performTextInput("75")
        rule.onNodeWithTag("age").performTextInput("30")
        rule.onNodeWithTag("submit").assertIsNotEnabled()
        rule.onNodeWithText("男").performClick()
        rule.onNodeWithTag("submit").assertIsEnabled()
        assertThat(form.sex).isEqualTo(Sex.MALE)
    }
}
