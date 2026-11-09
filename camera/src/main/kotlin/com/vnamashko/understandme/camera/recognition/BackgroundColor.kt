package com.vnamashko.understandme.camera.recognition

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot
import androidx.core.graphics.get

private const val SAMPLES_PER_EDGE = 24
private val RING_OFFSETS_PX = floatArrayOf(3f, 6f)

internal fun Bitmap.backgroundColorAround(corners: List<Offset>): Color? {
    if (corners.size < 3) return null
    val center = Offset(corners.map { it.x }.average().toFloat(), corners.map { it.y }.average().toFloat())
    val reds = mutableListOf<Int>()
    val greens = mutableListOf<Int>()
    val blues = mutableListOf<Int>()

    corners.indices.forEach { index ->
        val start = corners[index]
        val end = corners[(index + 1) % corners.size]
        val edge = end - start
        val length = hypot(edge.x, edge.y)
        if (length == 0f) return@forEach
        var normal = Offset(-edge.y / length, edge.x / length)
        val fromCenter = (start + end) / 2f - center
        if (fromCenter.x * normal.x + fromCenter.y * normal.y < 0f) normal = -normal

        for (step in 0 until SAMPLES_PER_EDGE) {
            val point = start + edge * ((step + 0.5f) / SAMPLES_PER_EDGE)
            for (ringOffset in RING_OFFSETS_PX) {
                val x = (point.x + normal.x * ringOffset).toInt()
                val y = (point.y + normal.y * ringOffset).toInt()
                if (x !in 0 until width || y !in 0 until height) continue
                val pixel = this[x, y]
                reds += (pixel shr 16) and 0xFF
                greens += (pixel shr 8) and 0xFF
                blues += pixel and 0xFF
            }
        }
    }

    if (reds.isEmpty()) return null
    return Color(red = reds.median(), green = greens.median(), blue = blues.median())
}

private fun MutableList<Int>.median(): Int {
    sort()
    return this[size / 2]
}
