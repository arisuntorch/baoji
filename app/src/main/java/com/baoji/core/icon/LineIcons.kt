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
 * Exercise icons are gym-sign figures: filled round head, line body, the equipment drawn plainly.
 * Colour comes from the caller's tint, so every icon works in light and dark.
 */
object LineIcons {

    /** A path starting with `*` is filled instead of stroked (heads and dots on the figures). */
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                if (d.startsWith("*")) {
                    addPath(pathData = addPathNodes(d.substring(1)), fill = SolidColor(Color.Black))
                } else {
                    addPath(
                        pathData = addPathNodes(d),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
        }.build()

    // ---- UI ----
    val Person = icon("person", "M12 12a4 4 0 1 0 0-8a4 4 0 1 0 0 8z", "M4.5 20.5c1.2-3.6 4-5.5 7.5-5.5s6.3 1.9 7.5 5.5")
    val Import = icon("import", "M12 3.5v11", "M7.5 10l4.5 4.5l4.5-4.5", "M4.5 16.5v3h15v-3")
    val Export = icon("export", "M12 14.5v-11", "M7.5 8l4.5-4.5l4.5 4.5", "M4.5 16.5v3h15v-3")
    val Back = icon("back", "M15 5l-7 7l7 7")
    val Forward = icon("forward", "M9 5l7 7l-7 7")
    val Down = icon("down", "M7 10l5 5l5-5")
    val Plus = icon("plus", "M12 5v14", "M5 12h14")
    val Minus = icon("minus", "M5 12h14")
    val Check = icon("check", "M5 12.5l4.5 4.5l9.5-10")
    val Dumbbell = icon("training", "M6.5 7v10", "M17.5 7v10", "M3.5 9.5v5", "M20.5 9.5v5", "M6.5 12h11")
    val Bowl = icon("diet", "M3.5 11.5h17c0 4.5-3.8 8-8.5 8s-8.5-3.5-8.5-8z", "M9 8.5c0-1.5 1-2 1-3.5", "M13.5 8.5c0-1.5 1-2 1-3.5")
    val Home = icon("home", "M4 11l8-6.5l8 6.5", "M6.5 9.5v10h11v-10")

    // ---- training ----
    private val trainingIcons: Map<TrainingIcon, ImageVector> = mapOf(
        TrainingIcon.BENCH to icon("bench", "*M2.5 12.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M3 15.5h14", "M5 15.5v4", "M15 15.5v4", "M7 13.6L13.5 13.6L17 10.5L19.5 19.5", "M9.6 13.6V7", "M4 7h11.2", "M5.4 5v4", "M13.8 5v4"),
        TrainingIcon.SQUAT to icon("squat", "*M10.5 4.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M5.4 7.8h12.4", "M6.6 5.8v4", "M16.6 5.8v4", "M11.4 7.8L8 13.8L14 14.8L12 20.5", "M12 20.5h3"),
        TrainingIcon.DEADLIFT to icon("deadlift", "*M14.5 6.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M14 9.3L8.2 11.6L10.6 15.6L9.4 20.5", "M14 9.3v6.6", "M11.6 18.1a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0z", "M3.5 20.5h17"),
        TrainingIcon.PRESS to icon("press", "*M9.9 8.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M4.5 3.5h15", "M6 2v3", "M18 2v3", "M8 3.5L8.6 7.5L9.8 11.4L14.2 11.4L15.4 7.5L16 3.5", "M12 11.4V16", "M10 21L12 16L14 21"),
        TrainingIcon.ROW to icon("row", "*M16.1 7.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M15.4 9.2L6 10.6L7.6 15.2L6.6 20.5", "M15.4 9.2L11.2 7.6L11.4 12.4", "M9.7 13.6h3.4", "M9.7 12.5v2.2", "M13.1 12.5v2.2"),
        TrainingIcon.PULLDOWN to icon("pulldown", "M12 2v4.5", "M4.5 8l2-1.5h11l2 1.5", "*M9.9 10.2a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M7.2 6.6L6.6 11L9.4 13", "M16.8 6.6L17.4 11L14.6 13", "M9.4 13h5.2", "M12 13v5.5", "M7.5 18.5h9", "M9.5 18.5v3", "M14.5 18.5v3"),
        TrainingIcon.PULL_UP to icon("pull_up", "M3 3.5h18", "*M9.9 7.8a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M7 3.5L6.8 8L9.6 10.4L14.4 10.4L17.2 8L17 3.5", "M12 10.4v5.8", "M10.6 21L12 16.2L13.4 21"),
        TrainingIcon.CURL to icon("curl", "*M8.5 4.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M10.6 7v7.2", "M9 20.5L10.6 14.2L12.2 20.5", "M11 8.2L11.6 12.6L15.2 8.8", "M15.4 7v3.2", "M14.3 7h2.2", "M14.3 10.2h2.2"),
        TrainingIcon.DIP to icon("dip", "M3.5 12.5h5", "M15.5 12.5h5", "M6 12.5v8.5", "M18 12.5v8.5", "*M9.9 6.8a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M6.5 12.5L9.4 9.6L14.6 9.6L17.5 12.5", "M12 9.6v6.2", "M10.2 19.6L12 15.8L13.8 19.6"),
        TrainingIcon.PUSHDOWN to icon("pushdown", "M20 2.5v18.5", "M15.4 3.5h4.6", "M15.4 3.5v11", "*M7.9 4.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M10 7.2v7", "M8.6 20.5L10 14.2L11.4 20.5", "M10.4 8.4L10.8 12.6L15.4 14.6", "M13.8 14.6h3.2"),
        TrainingIcon.FLY to icon("fly", "*M9.9 5a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M12 7.6v6.8", "M10 21L12 14.4L14 21", "M12 9c-3 0-5.5-0.6-7.4-2.8", "M12 9c3 0 5.5-0.6 7.4-2.8", "M4.6 4.8v2.8", "M3.5 4.8h2.2", "M3.5 7.6h2.2", "M19.4 4.8v2.8", "M18.3 4.8h2.2", "M18.3 7.6h2.2"),
        TrainingIcon.LATERAL_RAISE to icon("lateral_raise", "*M9.9 4.8a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M12 7.4v7", "M10 21L12 14.4L14 21", "M3.8 9h16.4", "M3.8 7.4v3.2", "M2.7 7.4h2.2", "M2.7 10.6h2.2", "M20.2 7.4v3.2", "M19.1 7.4h2.2", "M19.1 10.6h2.2"),
        TrainingIcon.FACE_PULL to icon("face_pull", "M20 2.5v18.5", "*M8.3 4.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M10.4 7.2v7", "M9 20.5L10.4 14.2L11.8 20.5", "M10.6 8.6L7.2 6L13.8 7.6", "M13.8 7.6L20 7"),
        TrainingIcon.LEG_PRESS to icon("leg_press", "M3.4 8.6L6 17h5", "M11 17l9.5-5.5", "*M2.3 6.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M5 9.4L7.2 15.6L10.2 8.2L15.8 10.8", "M14.4 8.4l2.8 4.8"),
        TrainingIcon.LEG_EXTENSION to icon("leg_extension", "M5.5 6v9.5h7", "M6.5 15.5v5", "M11.5 15.5v5", "*M6.3 4.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M8.2 7.2L8.2 14L13 14L19 11.6", "M18.2 13.4a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", "M8.4 9L11 12.4"),
        TrainingIcon.LEG_CURL to icon("leg_curl", "M2.5 15.5h13", "M4 15.5v5", "M14 15.5v5", "*M2.3 11.8a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M6.8 13.6h10.2", "M17 13.6L18.6 7.6", "M18.1 6.2a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0z"),
        TrainingIcon.CALF_RAISE to icon("calf_raise", "*M9.3 4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M11.4 6.6v7", "M11.4 13.6v3", "M11.4 16.6L14.2 18.6", "M13.6 18.6h6.4v2.4h-6.4z", "M6 18v-6", "M4.3 13.6l1.7-1.7l1.7 1.7"),
        TrainingIcon.HIP_THRUST to icon("hip_thrust", "M2.5 14h6", "M3.5 14v6.5", "M7.5 14v6.5", "*M2.5 11.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M7 12.6L17.4 12.6L17.4 20.5", "M10.1 10a2.3 2.3 0 1 0 4.6 0a2.3 2.3 0 1 0 -4.6 0z", "M15.6 20.5h3.4"),
        TrainingIcon.LUNGE to icon("lunge", "*M9.5 4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M11.6 6.6v6.4", "M11.6 13L16 13.6L16 20.5L18.2 20.5", "M11.6 13L8.6 19.4L5.4 20.5", "M11.8 8.2L11 12.4"),
        TrainingIcon.CRUNCH to icon("crunch", "M3 20.5h18", "*M4.3 12.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M11 19.5c-2.4 0-4-1.6-4.4-4.4", "M11 19.5L15 14L18.8 19.6", "M8.6 13.6L10.6 11.4"),
        TrainingIcon.PLANK to icon("plank", "M2.5 20.5h19", "*M2.5 13.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M6.6 15L20.2 19.2", "M6.6 15L6.6 19.6L9.6 19.6"),
        TrainingIcon.PUSH_UP to icon("push_up", "M2.5 20.5h19", "*M2.5 10.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M6.6 12L20.2 19.2", "M7.2 12.2L7.2 20.2"),
        TrainingIcon.JUMP_ROPE to icon("jump_rope", "*M9.9 4.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M12 7v6.6", "M10.4 19L12 13.6L13.6 19", "M12 8.4L7.8 12.6", "M12 8.4L16.2 12.6", "M7.8 12.6c-0.8 5.6 1.6 8.4 4.2 8.4s5-2.8 4.2-8.4"),
        TrainingIcon.CYCLE to icon("cycle", "M2.3 16.4a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0z", "M14.7 16.4a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0z", "M5.8 16.4L9.4 9.4L15.4 9.4L18.2 16.4", "M9.4 9.4L11.8 16.4L15.4 9.4", "M8 7.6h3", "M14.6 7.6h2.6"),
        TrainingIcon.SWIM to icon("swim", "*M15.1 8.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M15 11L6.2 13.8", "M15.4 10.8c-1.6-3.2-4.4-4.8-8.2-4.2", "M3 18c1.5 0 1.5-1.2 3-1.2s1.5 1.2 3 1.2s1.5-1.2 3-1.2s1.5 1.2 3 1.2s1.5-1.2 3-1.2s1.5 1.2 3 1.2"),
        TrainingIcon.KETTLEBELL to icon("kettlebell", "M8.4 11.6l-0.6-4a2 2 0 0 1 2-2.2h4.4a2 2 0 0 1 2 2.2l-0.6 4", "M6.9 15.4a5.1 5.1 0 1 0 10.2 0a5.1 5.1 0 1 0 -10.2 0z"),
        TrainingIcon.STRETCH to icon("stretch", "*M9.9 6.4a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M7.8 2.6L12 9.2L16.2 2.6", "M12 8.6v6", "M12 14.6v6.4", "M12 14.6L15.6 16.4L12.4 17.8"),
        TrainingIcon.RUN to icon("run", "*M13.3 4.2a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M14 6.8L11.2 12.6", "M11.2 12.6L15 15L13.6 20.4", "M11.2 12.6L9.4 16.4L5.4 16.8", "M13.4 8.2L16.6 10.2L18.6 8.4", "M13.4 8.2L10.4 9.4L8.4 8"),
        TrainingIcon.WALK to icon("walk", "*M10.5 4.2a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "M12.2 6.8L11.6 13", "M11.6 13L14.4 20.4", "M11.6 13L9.6 16.6L9 20.4", "M12.1 8.2L14.4 12", "M12.1 8.2L9.8 11.6"),
        TrainingIcon.DEFAULT to Dumbbell,
    )

    // ---- food ----
    private val foodIcons: Map<FoodIcon, ImageVector> = mapOf(
        FoodIcon.EGG to icon("egg", "M12 3.5c3.5 0 6 5.5 6 9.5a6 6 0 0 1-12 0c0-4 2.5-9.5 6-9.5z"),
        FoodIcon.MILK to icon("milk", "M9 3.5h6", "M9 3.5v3l-2 3v11h10v-11l-2-3v-3", "M7 12.5h10"),
        FoodIcon.YOGURT to icon("yogurt", "M5.5 8.5h13", "M6.5 8.5l1.2 10.6a1.6 1.6 0 0 0 1.6 1.4h5.4a1.6 1.6 0 0 0 1.6-1.4l1.2-10.6", "M6.6 8.5c0-2.6 2.4-4.4 5.4-4.4s5.4 1.8 5.4 4.4", "M7.2 13h9.6"),
        FoodIcon.CHEESE to icon("cheese", "M3.5 14.5L15.5 6.5l5 4v9.5h-17z", "M3.5 14.5h17", "M7.5 17.4a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z", "M13.7 16.8a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0z"),
        FoodIcon.PROTEIN to icon("protein", "M8.5 4h7v2.5", "M6.5 6.5h11v13.5h-11z", "M9.5 12h5", "M9.5 15h5"),
        FoodIcon.CHICKEN to icon("chicken", "M14.5 4.5a5 5 0 0 1 2 9.5l-4.5 2.5l-3 3", "M14.5 4.5a5 5 0 0 0-4.5 7.5", "M10 12l-2 5", "M9 19.5l-2 1l-1.5-1.5l1-2"),
        FoodIcon.BEEF to icon("beef", "M5.2 9.6c1.6-3.6 6.4-5.2 10.4-4s5.6 4.4 4.6 7.6s-4.8 6.8-9.6 6.8c-3.6 0-6.8-2-6.6-4.8c0.2-2 0.8-3.2 1.2-5.6z", "M7.4 10.4c1.4-2.4 4.6-3.6 7.6-2.8", "M12.9 13a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0z"),
        FoodIcon.FISH to icon("fish", "M3.5 12c2.6-4 7.4-5.4 11.4-3.4l1.6 1.4l3.5-2.4v8.8l-3.5-2.4l-1.6 1.4c-4 2-8.8 0.6-11.4-3.4z", "*M6.8 11a0.8 0.8 0 1 0 1.6 0a0.8 0.8 0 1 0 -1.6 0z", "M10.6 9.4c0.9 1.6 0.9 3.6 0 5.2"),
        FoodIcon.SHRIMP to icon("shrimp", "M8 5c6 0 10 3.2 10 7.2s-4 6.8-9 6.8", "M8 8.4c4 0 6.6 1.6 6.6 4s-2.4 3.8-5.6 3.8", "M8 5v3.4", "M9 19l-3.2 1.6", "M9 16.2l-3.2-1", "M14.6 6.6l-1.6 2.6", "M17.6 10.6l-3 1", "M17.4 14.6l-3-0.8", "M8 5c-2-0.4-3.6 0.4-4.6 1.6"),
        FoodIcon.SOY to icon("soy", "M12 4l7.5 4v8L12 20l-7.5-4V8z", "M4.5 8l7.5 4l7.5-4", "M12 12v8"),
        FoodIcon.RICE to icon("rice", "M3.5 11.5h17c0 4.5-3.8 8-8.5 8s-8.5-3.5-8.5-8z", "M7 11.5c0-3 2.2-5 5-5s5 2 5 5"),
        FoodIcon.NOODLE to icon("noodle", "M3.5 11.5h17c0 4.5-3.8 8-8.5 8s-8.5-3.5-8.5-8z", "M15 3.5l-4 8", "M18 4.5l-4.5 7", "M8.5 11.5c0-1.5 1-1.5 1-3"),
        FoodIcon.BREAD to icon("bread", "M5.5 10.5a3.5 3.5 0 0 1 2-6.5h9a3.5 3.5 0 0 1 2 6.5v9.5h-13z"),
        FoodIcon.DUMPLING to icon("dumpling", "M3.5 15.6a8.5 8.5 0 0 1 17 0c-2.8 1.8-14.2 1.8-17 0z", "M5.04 10.72L6.59 11.81", "M8.14 8.03L9 9.72", "M12 7.1L12 9", "M15.86 8.03L15 9.72", "M18.96 10.72L17.41 11.81"),
        FoodIcon.OATS to icon("oats", "M12 20.5v-15", "M12 9l-3-2.5", "M12 9l3-2.5", "M12 13l-3-2.5", "M12 13l3-2.5", "M12 17l-3-2.5", "M12 17l3-2.5"),
        FoodIcon.CORN to icon("corn", "M12 3c2.4 0 3.4 3 3.4 7s-1 7.6-3.4 7.6s-3.4-3.6-3.4-7.6s1-7 3.4-7z", "M8.8 7.8h6.4", "M8.6 11h6.8", "M9 14.2h6", "M12 3.4v14", "M8.6 10.4c-2.4 2.4-2.8 6.4-1 10c2-0.4 3.8-1.4 4.4-2.8", "M15.4 10.4c2.4 2.4 2.8 6.4 1 10c-2-0.4-3.8-1.4-4.4-2.8"),
        FoodIcon.POTATO to icon("potato", "M5.4 11.2c0.8-4 5.6-6 9.8-4.8s5.6 5.4 4 9.2s-7.2 5-11 3.2s-3.4-4.8-2.8-7.6z", "*M9.3 10.4a0.7 0.7 0 1 0 1.4 0a0.7 0.7 0 1 0 -1.4 0z", "*M13.9 11.6a0.7 0.7 0 1 0 1.4 0a0.7 0.7 0 1 0 -1.4 0z", "*M10.3 15.4a0.7 0.7 0 1 0 1.4 0a0.7 0.7 0 1 0 -1.4 0z"),
        FoodIcon.BANANA to icon("banana", "M5 13.5c4 4 11 3 13.5-5l1-3", "M5 13.5c1 3.5 7 6.5 12 2.5c1.5-1.5 2.5-4 2.5-7"),
        FoodIcon.APPLE to icon("apple", "M12 8c-1.6-1.2-3.4-1.6-5-1c-2.8 1-3.8 4.6-2.8 8c1 3.4 3.4 6 5.4 6c1.2 0 1.4-0.6 2.4-0.6s1.2 0.6 2.4 0.6c2 0 4.4-2.6 5.4-6c1-3.4 0-7-2.8-8c-1.6-0.6-3.4-0.2-5 1z", "M12 8c0-2 0.4-3.2 1.4-4.4"),
        FoodIcon.AVOCADO to icon("avocado", "M12 3.5c-2.6 0-3.6 3-4.6 6s-2.6 4.4-2.6 6.4a7.2 7.2 0 0 0 14.4 0c0-2-1.6-3.4-2.6-6.4s-2-6-4.6-6z", "M9.4 15.4a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0z"),
        FoodIcon.VEGETABLE to icon("vegetable", "M9.4 13.4a3.2 3.2 0 0 1-2.8-5.2a3.4 3.4 0 0 1 5.4-3.4a3.4 3.4 0 0 1 5.4 3.4a3.2 3.2 0 0 1-2.8 5.2z", "M10 13.4l1 7h2l1-7", "M12 16.4l-1.6-3"),
        FoodIcon.NUTS to icon("nuts", "M12 4.2a3.6 3.6 0 0 1 3.2 5.4c-0.4 0.6-0.4 1.2 0.2 1.8a4.2 4.2 0 1 1-6.8 0c0.6-0.6 0.6-1.2 0.2-1.8A3.6 3.6 0 0 1 12 4.2z", "*M10.4 7.6a0.6 0.6 0 1 0 1.2 0a0.6 0.6 0 1 0 -1.2 0z", "*M12.4 9a0.6 0.6 0 1 0 1.2 0a0.6 0.6 0 1 0 -1.2 0z", "*M10.4 14.6a0.6 0.6 0 1 0 1.2 0a0.6 0.6 0 1 0 -1.2 0z", "*M12.6 16.4a0.6 0.6 0 1 0 1.2 0a0.6 0.6 0 1 0 -1.2 0z"),
        FoodIcon.COFFEE to icon("coffee", "M4.5 10h11.5v4.6a4.8 4.8 0 0 1-4.8 4.8h-1.9a4.8 4.8 0 0 1-4.8-4.8z", "M16 11.4h1.4a2.2 2.2 0 0 1 0 4.4H16", "M8.6 3.6c-1 1.1 1 2.2 0 3.6", "M12 3.6c-1 1.1 1 2.2 0 3.6"),
        FoodIcon.WATER to icon("water", "M6 3.5h12l-1.6 16a1.6 1.6 0 0 1-1.6 1.4H9.2a1.6 1.6 0 0 1-1.6-1.4z", "M7 10.5c1.6-1 3.4-1 5 0s3.4 1 5 0"),
        FoodIcon.DEFAULT to Bowl,
    )

    fun training(icon: TrainingIcon): ImageVector = trainingIcons.getValue(icon)
    fun food(icon: FoodIcon): ImageVector = foodIcons.getValue(icon)
    fun forExercise(name: String): ImageVector = training(IconMatcher.training.match(name))
    fun forFood(name: String): ImageVector = food(IconMatcher.food.match(name))
}
