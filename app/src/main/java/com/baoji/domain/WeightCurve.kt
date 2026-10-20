package com.baoji.domain

/** A point on the weight chart: x = epochDay, y = kg. */
data class CurvePoint(val x: Double, val y: Double)

object WeightCurve {

    const val DEFAULT_ALPHA = 0.3

    /** Exponential moving average over points already sorted by x. */
    fun ema(points: List<CurvePoint>, alpha: Double = DEFAULT_ALPHA): List<CurvePoint> {
        require(alpha in 0.0..1.0) { "alpha must be in [0,1]" }
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<CurvePoint>(points.size)
        var acc = points.first().y
        points.forEach { p ->
            acc = alpha * p.y + (1 - alpha) * acc
            out += CurvePoint(p.x, acc)
        }
        out[0] = points.first()
        return out
    }

    /**
     * Monotone cubic (Fritsch–Carlson) interpolation. Returns [samplesPerSegment] points per
     * interval plus the last knot, so the drawn line never overshoots the data.
     */
    fun monotoneSpline(knots: List<CurvePoint>, samplesPerSegment: Int = 12): List<CurvePoint> {
        val n = knots.size
        if (n < 3) return knots
        val dx = DoubleArray(n - 1) { knots[it + 1].x - knots[it].x }
        val slope = DoubleArray(n - 1) { (knots[it + 1].y - knots[it].y) / dx[it] }
        val m = DoubleArray(n)
        m[0] = slope[0]
        m[n - 1] = slope[n - 2]
        for (i in 1 until n - 1) {
            m[i] = if (slope[i - 1] * slope[i] <= 0) 0.0 else (slope[i - 1] + slope[i]) / 2
        }
        for (i in 0 until n - 1) {
            if (slope[i] == 0.0) {
                m[i] = 0.0; m[i + 1] = 0.0
                continue
            }
            val a = m[i] / slope[i]
            val b = m[i + 1] / slope[i]
            val s = a * a + b * b
            if (s > 9) {
                val t = 3 / kotlin.math.sqrt(s)
                m[i] = t * a * slope[i]
                m[i + 1] = t * b * slope[i]
            }
        }
        val out = ArrayList<CurvePoint>((n - 1) * samplesPerSegment + 1)
        for (i in 0 until n - 1) {
            val h = dx[i]
            for (k in 0 until samplesPerSegment) {
                val t = k.toDouble() / samplesPerSegment
                val t2 = t * t
                val t3 = t2 * t
                val h00 = 2 * t3 - 3 * t2 + 1
                val h10 = t3 - 2 * t2 + t
                val h01 = -2 * t3 + 3 * t2
                val h11 = t3 - t2
                val y = h00 * knots[i].y + h10 * h * m[i] + h01 * knots[i + 1].y + h11 * h * m[i + 1]
                out += CurvePoint(knots[i].x + t * h, y)
            }
        }
        out += knots.last()
        return out
    }
}
