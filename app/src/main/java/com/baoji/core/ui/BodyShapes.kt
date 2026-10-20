package com.baoji.core.ui

import com.baoji.domain.Muscle

/**
 * Muscle-map geometry in a 100 × 220 box per figure. Every shape is the LEFT half (as seen);
 * the drawing mirrors it. Points are x, y pairs smoothed into a closed curve when drawn.
 * Prototyped and checked at several sizes before being copied here.
 */
internal object BodyShapes {
    const val WIDTH = 100f
    const val HEIGHT = 220f

    /** Head ellipse: centre x, centre y, radius x, radius y. */
    val HEAD = floatArrayOf(50f, 12.5f, 8f, 10.5f)

    /** Neck down the outside, back up the inside to the crotch. */
    val SILHOUETTE = floatArrayOf(
        45.5f, 22f, 44.5f, 29f, 39f, 31.5f, 31f, 33f, 25f, 36f, 21.5f, 42f, 20f, 52f, 18.5f, 64f,
        17f, 76f, 14.5f, 88f, 13f, 100f, 13.5f, 106f, 12f, 113f, 13.5f, 121f, 17.5f, 123f, 20.5f, 117f,
        21.5f, 107f, 24f, 94f, 26f, 82f, 28.5f, 70f, 31f, 60f, 33.5f, 72f, 35f, 86f, 33.5f, 98f,
        31.5f, 112f, 31.5f, 132f, 34f, 152f, 35.5f, 162f, 34f, 174f, 35f, 190f, 38.5f, 203f, 36f, 210f,
        38f, 214f, 45f, 213.5f, 45.5f, 204f, 46.5f, 188f, 47f, 172f, 46.5f, 162f, 48f, 145f, 49f, 128f,
        50f, 116f,
    )

    val FRONT: List<Pair<Muscle, FloatArray>> = listOf(
        Muscle.TRAPS to floatArrayOf(
            44.5f, 28.5f, 45.5f, 32f, 38f, 34.5f, 34f, 34f,
        ),
        Muscle.SHOULDERS to floatArrayOf(
            36f, 35f, 39f, 37.5f, 37f, 43f, 32.5f, 48.5f, 26.5f, 51f, 22.5f, 48f, 23f, 41f, 28f, 36.5f,
        ),
        Muscle.CHEST to floatArrayOf(
            40.5f, 36.5f, 49f, 37.5f, 49f, 53.5f, 43.5f, 56f, 37.5f, 54f, 34.5f, 49f, 36.5f, 41f,
        ),
        Muscle.BICEPS to floatArrayOf(
            23f, 53f, 28f, 51f, 31.5f, 52f, 31f, 61f, 28.5f, 70f, 25f, 76f, 21.5f, 74f, 20.5f, 63f,
        ),
        Muscle.FOREARMS to floatArrayOf(
            20.5f, 79f, 25.5f, 79f, 26.5f, 84f, 23.5f, 96f, 20.5f, 105f, 16.5f, 104f, 15.5f, 93f, 17f, 84f,
        ),
        Muscle.ABS to floatArrayOf(
            42.5f, 58.5f, 49f, 58f, 49f, 101f, 45f, 98.5f, 42.5f, 88f,
        ),
        Muscle.OBLIQUES to floatArrayOf(
            35f, 57f, 40.5f, 60f, 40.5f, 87f, 42f, 96f, 37f, 94f, 35.5f, 83f, 34f, 68f,
        ),
        Muscle.QUADS to floatArrayOf(
            34f, 103f, 39f, 104f, 44f, 111f, 46f, 124f, 46.5f, 140f, 44.5f, 154f, 41f, 159.5f, 37f, 158f,
            33.5f, 146f, 32.5f, 122f,
        ),
        Muscle.ADDUCTORS to floatArrayOf(
            45.5f, 108.5f, 48.5f, 113f, 48.5f, 124f, 47f, 134f, 46f, 121f,
        ),
        Muscle.CALVES to floatArrayOf(
            35.5f, 168f, 40f, 166.5f, 43f, 171f, 43.5f, 186f, 41f, 198f, 38.5f, 197f, 35.5f, 183f,
        ),
    )

    val BACK: List<Pair<Muscle, FloatArray>> = listOf(
        Muscle.TRAPS to floatArrayOf(
            50f, 27.5f, 45.5f, 30f, 36f, 34.5f, 40.5f, 39f, 46f, 46f, 50f, 57f,
        ),
        Muscle.SHOULDERS to floatArrayOf(
            36f, 35.5f, 39f, 38.5f, 37f, 43.5f, 32.5f, 48.5f, 26.5f, 51f, 22.5f, 48f, 23f, 41f, 28f, 36.5f,
        ),
        Muscle.TRICEPS to floatArrayOf(
            23f, 53f, 28f, 51f, 31.5f, 52f, 31.5f, 61f, 29f, 70f, 25.5f, 76f, 21.5f, 74f, 20.5f, 63f,
        ),
        Muscle.FOREARMS to floatArrayOf(
            20.5f, 79f, 25.5f, 79f, 26.5f, 84f, 23.5f, 96f, 20.5f, 105f, 16.5f, 104f, 15.5f, 93f, 17f, 84f,
        ),
        Muscle.LATS to floatArrayOf(
            37.5f, 42f, 44.5f, 48f, 47.5f, 62f, 45.5f, 77f, 40f, 85f, 36f, 76f, 34.5f, 58f,
        ),
        Muscle.LOWER_BACK to floatArrayOf(
            47.5f, 66f, 50f, 61f, 50f, 97f, 44.5f, 96.5f, 43f, 89f, 47f, 79f,
        ),
        Muscle.GLUTES to floatArrayOf(
            35.5f, 99.5f, 50f, 100.5f, 50f, 116f, 45f, 121f, 37.5f, 120f, 33.5f, 110f,
        ),
        Muscle.HAMSTRINGS to floatArrayOf(
            33f, 123.5f, 44f, 124.5f, 47.5f, 131f, 46.5f, 149f, 43f, 158.5f, 38.5f, 159f, 34.5f, 148f,
            32.5f, 135f,
        ),
        Muscle.CALVES to floatArrayOf(
            34.5f, 165.5f, 41f, 164f, 45f, 170.5f, 44.5f, 185f, 41.5f, 195f, 37.5f, 194f, 34f, 180f,
        ),
    )

    /** Short cuts across the abs, drawn in the body colour. */
    val ABS_GAPS: List<FloatArray> = listOf(
        floatArrayOf(43f, 70f, 49f, 69.5f),
        floatArrayOf(43f, 80.5f, 49f, 80f),
        floatArrayOf(43f, 90.5f, 49f, 90f),
    )
}
