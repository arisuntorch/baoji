package com.baoji.core.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Built-in single-line icon set: 24×24 viewport, 1.5px stroke, round caps and joins.
 * Colour comes from the caller's tint, so every icon works in light and dark.
 */
object LineIcons {

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    // ---- UI ----
    val Person = icon("person", "M12 12a4 4 0 1 0 0-8a4 4 0 1 0 0 8z", "M4.5 20.5c1.2-3.6 4-5.5 7.5-5.5s6.3 1.9 7.5 5.5")
    val Import = icon("import", "M12 3.5v11", "M7.5 10l4.5 4.5l4.5-4.5", "M4.5 16.5v3h15v-3")
    val Export = icon("export", "M12 14.5v-11", "M7.5 8l4.5-4.5l4.5 4.5", "M4.5 16.5v3h15v-3")
    val Back = icon("back", "M15 5l-7 7l7 7")
    val Plus = icon("plus", "M12 5v14", "M5 12h14")
    val Minus = icon("minus", "M5 12h14")
    val Check = icon("check", "M5 12.5l4.5 4.5l9.5-10")
    val Dumbbell = icon("training", "M6.5 7v10", "M17.5 7v10", "M3.5 9.5v5", "M20.5 9.5v5", "M6.5 12h11")
    val Bowl = icon("diet", "M3.5 11.5h17c0 4.5-3.8 8-8.5 8s-8.5-3.5-8.5-8z", "M9 8.5c0-1.5 1-2 1-3.5", "M13.5 8.5c0-1.5 1-2 1-3.5")
    val Home = icon("home", "M4 11l8-6.5l8 6.5", "M6.5 9.5v10h11v-10")

    // ---- training ----
    private val trainingIcons: Map<TrainingIcon, ImageVector> = mapOf(
        TrainingIcon.BENCH to icon("bench", "M3.5 17h17", "M6 17v3", "M18 17v3", "M5 9.5h14", "M6.5 7v5", "M17.5 7v5", "M10 14.5a2 2 0 1 0 4 0"),
        TrainingIcon.SQUAT to icon("squat", "M4 6h16", "M6 4v4", "M18 4v4", "M12 6v5l-3.5 3.5l3.5 5.5", "M12 11l3.5 3.5l-3.5 5.5"),
        TrainingIcon.DEADLIFT to icon("deadlift", "M4 18.5h16", "M6 16v5", "M18 16v5", "M12 4a1.5 1.5 0 1 0 0.01 0", "M12 7v6l-3 5.5", "M12 13l3 5.5", "M9 10l-1.5 8.5", "M15 10l1.5 8.5"),
        TrainingIcon.PRESS to icon("press", "M4 4.5h16", "M6 2.5v4", "M18 2.5v4", "M8 4.5l1.5 6h5l1.5-6", "M12 10.5v9.5", "M9.5 20h5"),
        TrainingIcon.ROW to icon("row", "M3.5 18h7", "M7 15v6", "M10.5 18l6-8", "M16.5 10l3-1.5", "M14 5.5a1.5 1.5 0 1 0 0.01 0"),
        TrainingIcon.PULLDOWN to icon("pulldown", "M3.5 4.5h17", "M12 4.5v4", "M6.5 8.5h11", "M8 8.5l2 6.5", "M16 8.5l-2 6.5", "M10 15h4v5.5"),
        TrainingIcon.CURL to icon("curl", "M7 20.5v-8.5", "M7 12l6.5-6", "M11.5 4l4 4", "M5 20.5h4"),
        TrainingIcon.DIP to icon("dip", "M4.5 9h4", "M15.5 9h4", "M6.5 9v11", "M17.5 9v11", "M12 4.5a1.5 1.5 0 1 0 0.01 0", "M8.5 9l3.5 2.5l3.5-2.5", "M12 11.5v5"),
        TrainingIcon.FLY to icon("fly", "M12 6.5v12", "M12 9.5c-2.5 0-5-1-7.5-3.5", "M12 9.5c2.5 0 5-1 7.5-3.5", "M3.5 5l2 2", "M20.5 5l-2 2"),
        TrainingIcon.LEG_PRESS to icon("legpress", "M4 19.5l5-5", "M9 14.5h4.5l3-6", "M14.5 6.5l4 4", "M4 19.5h6"),
        TrainingIcon.RUN to icon("run", "M14.5 4.5a1.5 1.5 0 1 0 0.01 0", "M9 8.5l4-1l2.5 3.5l3 1", "M13 7.5l-2 5.5l3.5 3l-1 4.5", "M11 13l-3.5 3.5h-3"),
        TrainingIcon.WALK to icon("walk", "M12.5 4a1.5 1.5 0 1 0 0.01 0", "M12 7.5v6.5l-2.5 6", "M12 14l2.5 6", "M12 9.5l-3 2.5", "M12 9.5l3 2"),
        TrainingIcon.DEFAULT to Dumbbell,
    )

    // ---- food ----
    private val foodIcons: Map<FoodIcon, ImageVector> = mapOf(
        FoodIcon.EGG to icon("egg", "M12 3.5c3.5 0 6 5.5 6 9.5a6 6 0 0 1-12 0c0-4 2.5-9.5 6-9.5z"),
        FoodIcon.MILK to icon("milk", "M9 3.5h6", "M9 3.5v3l-2 3v11h10v-11l-2-3v-3", "M7 12.5h10"),
        FoodIcon.PROTEIN to icon("protein", "M7.5 6.5h9", "M8.5 4h7v2.5", "M6.5 6.5h11v13.5h-11z", "M9.5 12h5", "M9.5 15h5"),
        FoodIcon.CHICKEN to icon("chicken", "M14.5 4.5a5 5 0 0 1 2 9.5l-4.5 2.5l-3 3", "M14.5 4.5a5 5 0 0 0-4.5 7.5", "M10 12l-2 5", "M9 19.5l-2 1l-1.5-1.5l1-2"),
        FoodIcon.BEEF to icon("beef", "M4.5 12c0-4.5 4-7.5 8.5-7.5s6.5 3.5 6.5 6.5c0 5-5 8.5-10 8.5c-3 0-5-3-5-7.5z", "M10.5 10.5a2 2 0 1 0 0.01 0"),
        FoodIcon.FISH to icon("fish", "M3.5 12c3-4.5 9-5.5 13-1.5c-4 4-10 3-13-1.5", "M16.5 10.5l4-3v9l-4-3", "M8 11.5h0.01"),
        FoodIcon.RICE to icon("rice", "M3.5 11.5h17c0 4.5-3.8 8-8.5 8s-8.5-3.5-8.5-8z", "M7 11.5c0-3 2.2-5 5-5s5 2 5 5"),
        FoodIcon.NOODLE to icon("noodle", "M3.5 11.5h17c0 4.5-3.8 8-8.5 8s-8.5-3.5-8.5-8z", "M15 3.5l-4 8", "M18 4.5l-4.5 7", "M8.5 11.5c0-1.5 1-1.5 1-3"),
        FoodIcon.BREAD to icon("bread", "M5.5 10.5a3.5 3.5 0 0 1 2-6.5h9a3.5 3.5 0 0 1 2 6.5v9.5h-13z"),
        FoodIcon.OATS to icon("oats", "M12 20.5v-15", "M12 9l-3-2.5", "M12 9l3-2.5", "M12 13l-3-2.5", "M12 13l3-2.5", "M12 17l-3-2.5", "M12 17l3-2.5"),
        FoodIcon.BANANA to icon("banana", "M5 13.5c4 4 11 3 13.5-5l1-3", "M5 13.5c1 3.5 7 6.5 12 2.5c1.5-1.5 2.5-4 2.5-7"),
        FoodIcon.VEGETABLE to icon("vegetable", "M12 20.5v-7", "M12 13.5a3.5 3.5 0 1 1-3-5.5a3.5 3.5 0 0 1 6 0a3.5 3.5 0 1 1-3 5.5z"),
        FoodIcon.DEFAULT to Bowl,
    )

    fun training(icon: TrainingIcon): ImageVector = trainingIcons.getValue(icon)
    fun food(icon: FoodIcon): ImageVector = foodIcons.getValue(icon)
    fun forExercise(name: String): ImageVector = training(IconMatcher.training.match(name))
    fun forFood(name: String): ImageVector = food(IconMatcher.food.match(name))
}
