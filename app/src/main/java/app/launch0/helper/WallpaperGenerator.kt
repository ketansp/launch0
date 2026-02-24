package app.launch0.helper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
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
        0xFF1e3a5f.toInt(), // steel blue
        0xFF4a1942.toInt(), // plum
        0xFF0f5257.toInt(), // deep teal
        0xFF7b2d8e.toInt(), // vivid purple
        0xFF1f4037.toInt(), // dark emerald
        0xFF6b2fa0.toInt(), // bright violet
        0xFF0d4f8b.toInt(), // ocean blue
        0xFF8b1a4a.toInt(), // raspberry
        0xFF2e4057.toInt(), // blue-grey
        0xFFb02a5e.toInt(), // magenta-rose
        0xFF1a6b4f.toInt(), // jewel green
        0xFF4834d4.toInt(), // indigo
        0xFFc44569.toInt(), // coral-pink
        0xFF0c7b93.toInt(), // bright teal
        0xFF8854d0.toInt(), // medium purple
        0xFF2d6a4f.toInt(), // forest green
    )

    private val lightPalette = intArrayOf(
        0xFF7eb8da.toInt(), // sky blue
        0xFFe8a87c.toInt(), // peach
        0xFFd4a5e5.toInt(), // orchid
        0xFF85c7a2.toInt(), // mint green
        0xFFf0a1b7.toInt(), // rose pink
        0xFF8fd3e2.toInt(), // aqua
        0xFFf5c16c.toInt(), // warm amber
        0xFFb5a8d5.toInt(), // soft violet
        0xFFf09ea7.toInt(), // salmon
        0xFF82c4b5.toInt(), // sea foam
        0xFFe0a3d0.toInt(), // mauve
        0xFFa8d5ba.toInt(), // sage green
        0xFFf4b886.toInt(), // tangerine
        0xFF92b4d8.toInt(), // periwinkle
        0xFFe8c170.toInt(), // gold
        0xFFc49bc4.toInt(), // plum pink
    )

    fun generate(width: Int, height: Int, isDark: Boolean, seed: Long): android.graphics.Bitmap {
        val random = Random(seed)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        canvas.drawColor(if (isDark) 0xFF0a0a14.toInt() else 0xFFF5F0EB.toInt())

        val palette = if (isDark) darkPalette else lightPalette
        val patternType = random.nextInt(4)

        when (patternType) {
            0 -> drawLayeredLinearGradients(canvas, width, height, palette, random)
            1 -> drawDiagonalFlow(canvas, width, height, palette, random)
            2 -> drawNebula(canvas, width, height, palette, isDark, random)
            3 -> drawPrism(canvas, width, height, palette, isDark, random)
        }

        return bitmap
    }

    private fun pickColors(palette: IntArray, count: Int, random: Random): IntArray {
        val indices = palette.indices.shuffled(random)
        return IntArray(count) { palette[indices[it]] }
    }

    /**
     * Pattern 0: Multiple overlapping linear gradients at random angles.
     */
    private fun drawLayeredLinearGradients(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, random: Random
    ) {
        val layerCount = 4 + random.nextInt(3)
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
                alpha = 160 + random.nextInt(80)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    /**
     * Pattern 1: Bold diagonal color flow using offset linear gradients.
     */
    private fun drawDiagonalFlow(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, random: Random
    ) {
        val colors = pickColors(palette, 4 + random.nextInt(3), random)
        val shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { this.shader = shader }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Stronger cross-gradient for richer color mixing
        val crossColors = pickColors(palette, 3 + random.nextInt(2), random)
        val crossShader = LinearGradient(
            w.toFloat(), 0f, 0f, h.toFloat(),
            crossColors, null, Shader.TileMode.CLAMP
        )
        val crossPaint = Paint().apply {
            this.shader = crossShader
            alpha = 130 + random.nextInt(60)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), crossPaint)

        // Third layer for even more mixing
        val thirdColors = pickColors(palette, 2, random)
        val angle = random.nextDouble() * 2 * Math.PI
        val len = maxOf(w, h).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val thirdShader = LinearGradient(
            cx - (len * cos(angle) / 2).toFloat(),
            cy - (len * sin(angle) / 2).toFloat(),
            cx + (len * cos(angle) / 2).toFloat(),
            cy + (len * sin(angle) / 2).toFloat(),
            thirdColors, null, Shader.TileMode.CLAMP
        )
        val thirdPaint = Paint().apply {
            this.shader = thirdShader
            alpha = 100 + random.nextInt(50)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), thirdPaint)
    }

    /**
     * Pattern 2: Dense overlapping color clouds creating a nebula effect.
     * Multiple large radial gradients blended over a colored base.
     */
    private fun drawNebula(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, isDark: Boolean, random: Random
    ) {
        // Rich two-color base gradient
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
            alpha = if (isDark) 200 else 220
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Many overlapping radial clouds for dense color mixing
        val cloudCount = 8 + random.nextInt(5)
        for (i in 0 until cloudCount) {
            val cx = random.nextFloat() * w * 1.2f - w * 0.1f
            val cy = random.nextFloat() * h * 1.2f - h * 0.1f
            val radius = minOf(w, h) * (0.3f + random.nextFloat() * 0.5f)
            val colors = pickColors(palette, 2, random)
            val midColor = blendColor(colors[0], colors[1], 0.5f)

            val shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(colors[0], midColor, Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                this.shader = shader
                alpha = 120 + random.nextInt(80)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    /**
     * Pattern 3: Sweep gradient centered off-screen with layered linear accents,
     * creating a prismatic color spread.
     */
    private fun drawPrism(
        canvas: Canvas, w: Int, h: Int, palette: IntArray, isDark: Boolean, random: Random
    ) {
        // Sweep gradient with multiple colors from an off-center point
        val sweepColors = pickColors(palette, 5 + random.nextInt(3), random)
        // Close the loop by repeating the first color
        val loopedColors = IntArray(sweepColors.size + 1).also {
            sweepColors.copyInto(it)
            it[sweepColors.size] = sweepColors[0]
        }

        val pivotX = w * (0.2f + random.nextFloat() * 0.6f)
        val pivotY = h * (0.2f + random.nextFloat() * 0.6f)

        val sweepShader = SweepGradient(pivotX, pivotY, loopedColors, null)
        val sweepPaint = Paint().apply {
            shader = sweepShader
            alpha = if (isDark) 200 else 230
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sweepPaint)

        // Soften with a radial fade from the pivot point
        val fadeColor = if (isDark) 0xFF0a0a14.toInt() else 0xFFF5F0EB.toInt()
        val fadeRadius = maxOf(w, h) * 0.9f
        val fadeShader = RadialGradient(
            pivotX, pivotY, fadeRadius,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, fadeColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val fadePaint = Paint().apply {
            shader = fadeShader
            alpha = 140 + random.nextInt(60)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fadePaint)

        // Add 1-2 linear gradient overlays for depth
        val overlayCount = 1 + random.nextInt(2)
        for (i in 0 until overlayCount) {
            val colors = pickColors(palette, 2, random)
            val angle = random.nextDouble() * 2 * Math.PI
            val len = maxOf(w, h).toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val overlayShader = LinearGradient(
                cx - (len * cos(angle) / 2).toFloat(),
                cy - (len * sin(angle) / 2).toFloat(),
                cx + (len * cos(angle) / 2).toFloat(),
                cy + (len * sin(angle) / 2).toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
            val overlayPaint = Paint().apply {
                shader = overlayShader
                alpha = 60 + random.nextInt(50)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlayPaint)
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
