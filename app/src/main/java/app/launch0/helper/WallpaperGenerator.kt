package app.launch0.helper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates random abstract wallpapers using gradients and mathematical patterns.
 * Uses a date-based seed so the wallpaper changes daily but stays consistent within a day.
 */
object WallpaperGenerator {

    private val darkPalette = intArrayOf(
        0xFF1a1a2e.toInt(),
        0xFF16213e.toInt(),
        0xFF0f3460.toInt(),
        0xFF533483.toInt(),
        0xFF2c003e.toInt(),
        0xFF1b4332.toInt(),
        0xFF003049.toInt(),
        0xFF370617.toInt(),
        0xFF6a040f.toInt(),
        0xFF0b525b.toInt(),
        0xFF3c096c.toInt(),
        0xFF240046.toInt(),
        0xFF012a4a.toInt(),
        0xFF1b1b3a.toInt(),
        0xFF2d1b69.toInt(),
        0xFF0d1b2a.toInt(),
    )

    private val lightPalette = intArrayOf(
        0xFFe8d5b7.toInt(),
        0xFFf2e9e4.toInt(),
        0xFFc9ada7.toInt(),
        0xFFa2d2ff.toInt(),
        0xFFbde0fe.toInt(),
        0xFFffd6ff.toInt(),
        0xFFe7c6ff.toInt(),
        0xFFc8b6ff.toInt(),
        0xFFffc8dd.toInt(),
        0xFFcaf0f8.toInt(),
        0xFFd8e2dc.toInt(),
        0xFFf1faee.toInt(),
        0xFFfefae0.toInt(),
        0xFFdda15e.toInt(),
        0xFFf4a261.toInt(),
        0xFFe9c46a.toInt(),
    )

    fun generate(width: Int, height: Int, isDark: Boolean, seed: Long): android.graphics.Bitmap {
        val random = Random(seed)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        canvas.drawColor(if (isDark) Color.BLACK else Color.WHITE)

        val palette = if (isDark) darkPalette else lightPalette
        val patternType = random.nextInt(5)

        when (patternType) {
            0 -> drawLayeredLinearGradients(canvas, width, height, palette, random)
            1 -> drawRadialOrbs(canvas, width, height, palette, isDark, random)
            2 -> drawDiagonalFlow(canvas, width, height, palette, random)
            3 -> drawAuroraWaves(canvas, width, height, palette, isDark, random)
            4 -> drawMeshGlow(canvas, width, height, palette, isDark, random)
        }

        return bitmap
    }

    private fun pickColors(palette: IntArray, count: Int, random: Random): IntArray {
        val indices = palette.indices.shuffled(random)
        return IntArray(count) { palette[indices[it]] }
    }

    /**
     * Pattern 0: Multiple overlapping linear gradients at random angles with varying opacity.
     */
    private fun drawLayeredLinearGradients(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, random: Random
    ) {
        val layerCount = 3 + random.nextInt(3)
        for (i in 0 until layerCount) {
            val colorCount = 2 + random.nextInt(2)
            val colors = pickColors(palette, colorCount, random)
            val angle = random.nextDouble() * 2 * Math.PI
            val cx = w / 2f
            val cy = h / 2f
            val len = maxOf(w, h).toFloat()
            val x0 = cx - (len * cos(angle) / 2).toFloat()
            val y0 = cy - (len * sin(angle) / 2).toFloat()
            val x1 = cx + (len * cos(angle) / 2).toFloat()
            val y1 = cy + (len * sin(angle) / 2).toFloat()

            val shader = LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP)
            val paint = Paint().apply {
                this.shader = shader
                alpha = 100 + random.nextInt(100)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    /**
     * Pattern 1: Soft glowing radial circles at random positions.
     */
    private fun drawRadialOrbs(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, isDark: Boolean, random: Random
    ) {
        val orbCount = 4 + random.nextInt(4)
        for (i in 0 until orbCount) {
            val cx = random.nextFloat() * w
            val cy = random.nextFloat() * h
            val radius = (minOf(w, h) * (0.3f + random.nextFloat() * 0.5f))
            val color = pickColors(palette, 1, random)[0]
            val transparent = if (isDark) Color.BLACK else Color.WHITE

            val shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(color, blendColor(color, transparent, 0.5f), transparent),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                this.shader = shader
                alpha = 140 + random.nextInt(80)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    /**
     * Pattern 2: Diagonal color flow using offset linear gradients.
     */
    private fun drawDiagonalFlow(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, random: Random
    ) {
        val colors = pickColors(palette, 4 + random.nextInt(2), random)
        // Main diagonal gradient
        val shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { this.shader = shader }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Add subtle cross-gradient for depth
        val crossColors = pickColors(palette, 3, random)
        val crossShader = LinearGradient(
            w.toFloat(), 0f, 0f, h.toFloat(),
            crossColors, null, Shader.TileMode.CLAMP
        )
        val crossPaint = Paint().apply {
            this.shader = crossShader
            alpha = 80 + random.nextInt(60)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), crossPaint)
    }

    /**
     * Pattern 3: Horizontal wavy color bands inspired by aurora borealis.
     */
    private fun drawAuroraWaves(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, isDark: Boolean, random: Random
    ) {
        val bandCount = 3 + random.nextInt(3)
        val baseColor = if (isDark) Color.BLACK else Color.WHITE

        for (i in 0 until bandCount) {
            val color = pickColors(palette, 1, random)[0]
            val yCenter = h * (0.1f + random.nextFloat() * 0.8f)
            val bandHeight = h * (0.15f + random.nextFloat() * 0.2f)

            val shader = LinearGradient(
                0f, yCenter - bandHeight, 0f, yCenter + bandHeight,
                intArrayOf(baseColor, color, color, baseColor),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )

            // Add horizontal variation with a secondary gradient
            val hColor = pickColors(palette, 1, random)[0]
            val xOffset = random.nextFloat() * w

            val paint = Paint().apply {
                this.shader = shader
                alpha = 120 + random.nextInt(80)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

            // Horizontal tint overlay for wave-like color shift
            val hShader = LinearGradient(
                xOffset - w * 0.3f, yCenter, xOffset + w * 0.3f, yCenter,
                intArrayOf(baseColor, hColor, baseColor),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            val hPaint = Paint().apply {
                this.shader = hShader
                alpha = 60 + random.nextInt(40)
            }
            canvas.drawRect(0f, yCenter - bandHeight, w.toFloat(), yCenter + bandHeight, hPaint)
        }
    }

    /**
     * Pattern 4: Scattered radial glows creating a mesh-like soft pattern.
     */
    private fun drawMeshGlow(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, isDark: Boolean, random: Random
    ) {
        // Base gradient
        val baseColors = pickColors(palette, 3, random)
        val baseAngle = random.nextDouble() * 2 * Math.PI
        val baseShader = LinearGradient(
            (w / 2 - w * cos(baseAngle) / 2).toFloat(),
            (h / 2 - h * sin(baseAngle) / 2).toFloat(),
            (w / 2 + w * cos(baseAngle) / 2).toFloat(),
            (h / 2 + h * sin(baseAngle) / 2).toFloat(),
            baseColors, null, Shader.TileMode.CLAMP
        )
        val basePaint = Paint().apply {
            shader = baseShader
            alpha = 180
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Overlay scattered radial glows
        val glowCount = 5 + random.nextInt(4)
        val transparent = if (isDark) Color.BLACK else Color.WHITE
        for (i in 0 until glowCount) {
            val cx = random.nextFloat() * w
            val cy = random.nextFloat() * h
            val radius = minOf(w, h) * (0.2f + random.nextFloat() * 0.3f)
            val color = pickColors(palette, 1, random)[0]

            val shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(color, transparent),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                this.shader = shader
                alpha = 80 + random.nextInt(60)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    private fun blendColor(color1: Int, color2: Int, ratio: Float): Int {
        val r = ((Color.red(color1) * (1 - ratio) + Color.red(color2) * ratio)).toInt()
        val g = ((Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio)).toInt()
        val b = ((Color.blue(color1) * (1 - ratio) + Color.blue(color2) * ratio)).toInt()
        val a = ((Color.alpha(color1) * (1 - ratio) + Color.alpha(color2) * ratio)).toInt()
        return Color.argb(a, r, g, b)
    }
}
