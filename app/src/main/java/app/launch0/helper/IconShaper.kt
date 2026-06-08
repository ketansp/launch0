package app.launch0.helper

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import app.launch0.data.Constants
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** App icon for a home/drawer entry, masked to [shape] and rendered at [sizePx]. */
fun Context.getShapedAppIcon(packageName: String, userString: String, sizePx: Int, shape: Int): Drawable? {
    if (sizePx <= 0) return null
    val icon = getAppIcon(packageName, userString) ?: return null
    return icon.toShapedIcon(resources, sizePx, shape)
}

/** Rasterizes the drawable at [sizePx] and clips it to the given [shape] with anti-aliased edges. */
fun Drawable.toShapedIcon(resources: Resources, sizePx: Int, shape: Int): Drawable {
    if (sizePx <= 0) return this

    // Rasterize the source drawable at the target size.
    val source = createBitmap(sizePx, sizePx)
    setBounds(0, 0, sizePx, sizePx)
    draw(Canvas(source))

    if (shape == Constants.IconShape.DEFAULT) {
        return BitmapDrawable(resources, source)
    }

    val output = createBitmap(sizePx, sizePx)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    Canvas(output).drawPath(shapePath(shape, sizePx.toFloat()), paint)
    return BitmapDrawable(resources, output)
}

private fun shapePath(shape: Int, size: Float): Path {
    val path = Path()
    val rect = RectF(0f, 0f, size, size)
    val r = size / 2f
    when (shape) {
        Constants.IconShape.CIRCLE -> path.addCircle(r, r, r, Path.Direction.CW)
        Constants.IconShape.SQUARE -> path.addRect(rect, Path.Direction.CW)
        Constants.IconShape.TEARDROP -> {
            // Three fully rounded corners + one sharp corner (bottom-right).
            val radii = floatArrayOf(r, r, r, r, 0f, 0f, r, r)
            path.addRoundRect(rect, radii, Path.Direction.CW)
        }
        Constants.IconShape.SQUIRCLE -> {
            // Superellipse |x|^n + |y|^n = 1 with n = 4 → squircle.
            val n = 4.0
            val steps = 72
            for (i in 0..steps) {
                val t = 2.0 * Math.PI * i / steps
                val ct = cos(t)
                val st = sin(t)
                val x = r + r * signOf(ct) * abs(ct).pow(2.0 / n)
                val y = r + r * signOf(st) * abs(st).pow(2.0 / n)
                if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
                else path.lineTo(x.toFloat(), y.toFloat())
            }
            path.close()
        }
        else -> path.addRect(rect, Path.Direction.CW)
    }
    return path
}

private fun signOf(value: Double): Double = if (value >= 0) 1.0 else -1.0
