package com.vnamashko.undertsndme.language.picker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Swap: ImageVector
    get() {
        if (_Swap != null) {
            return _Swap!!
        }
        _Swap = ImageVector.Builder(
            name = "Swap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            val path = path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(280f, 800f)
                lineTo(80f, 600f)
                lineToRelative(200f, -200f)
                lineToRelative(56f, 57f)
                lineToRelative(-103f, 103f)
                horizontalLineToRelative(287f)
                verticalLineToRelative(80f)
                horizontalLineTo(233f)
                lineToRelative(103f, 103f)
                close()
                moveToRelative(400f, -240f)
                lineToRelative(-56f, -57f)
                lineToRelative(103f, -103f)
                horizontalLineTo(440f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(287f)
                lineTo(624f, 217f)
                lineToRelative(56f, -57f)
                lineToRelative(200f, 200f)
                close()
            }
        }.build()
        return _Swap!!
    }

private var _Swap: ImageVector? = null