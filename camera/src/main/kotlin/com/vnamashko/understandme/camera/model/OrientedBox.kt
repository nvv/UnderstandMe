package com.vnamashko.understandme.camera.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val OVERLAP_TOLERANCE_PX = 1f

data class OrientedBox(
    val origin: Offset,
    val width: Float,
    val height: Float,
    val angleDegrees: Float,
) {
    private val angleRadians: Double get() = Math.toRadians(angleDegrees.toDouble())

    val along: Offset get() = Offset(cos(angleRadians).toFloat(), sin(angleRadians).toFloat())

    val down: Offset get() = Offset(-sin(angleRadians).toFloat(), cos(angleRadians).toFloat())

    fun corners(): List<Offset> = listOf(
        origin,
        origin + along * width,
        origin + along * width + down * height,
        origin + down * height,
    )

    fun grownBy(factor: Float): OrientedBox = copy(width = width * factor, height = height * factor)

    fun overlaps(other: OrientedBox): Boolean {
        val own = corners()
        val others = other.corners()
        return listOf(along, down, other.along, other.down).all { axis ->
            val first = own.projectedOn(axis)
            val second = others.projectedOn(axis)
            min(first.endInclusive, second.endInclusive) - max(first.start, second.start) > OVERLAP_TOLERANCE_PX
        }
    }

    fun isInside(bounds: Rect): Boolean = corners().all { corner ->
        corner.x >= bounds.left - OVERLAP_TOLERANCE_PX &&
            corner.x <= bounds.right + OVERLAP_TOLERANCE_PX &&
            corner.y >= bounds.top - OVERLAP_TOLERANCE_PX &&
            corner.y <= bounds.bottom + OVERLAP_TOLERANCE_PX
    }
}

fun RecognizedTextBlock.orientedBox(): OrientedBox {
    val topLeft = corners[0]
    val top = corners[1] - topLeft
    val left = corners[3] - topLeft
    return OrientedBox(
        origin = topLeft,
        width = top.getDistance(),
        height = left.getDistance(),
        angleDegrees = Math.toDegrees(atan2(top.y, top.x).toDouble()).toFloat(),
    )
}

private fun List<Offset>.projectedOn(axis: Offset): ClosedFloatingPointRange<Float> {
    val projections = map { it.x * axis.x + it.y * axis.y }
    return projections.min()..projections.max()
}
