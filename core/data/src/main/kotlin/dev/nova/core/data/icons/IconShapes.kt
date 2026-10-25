package dev.nova.core.data.icons

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import dev.nova.core.domain.model.IconShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Aplica um formato próprio a ícones adaptativos. As duas camadas (108dp) são desenhadas com a área visível
 * (72dp) ocupando o bitmap inteiro e depois recortadas por uma máscara com antialias (DST_IN); `clipPath`
 * sozinho deixaria a borda serrilhada em canvas de software.
 */
internal object IconShapes {
    private const val LAYER_RATIO = 108f / 72f
    private const val ROUNDED_RADIUS = 0.22f
    private const val SQUIRCLE_STEPS = 128
    private const val SQUIRCLE_EXPONENT = 0.5 // superelipse de grau 4: |x|^(2/4)

    fun drawMasked(canvas: Canvas, icon: AdaptiveIconDrawable, size: Int, shape: IconShape) {
        val s = size.toFloat()
        val inset = ((size * LAYER_RATIO).roundToInt() - size) / 2
        val layer = canvas.saveLayer(0f, 0f, s, s, null)
        for (part in listOfNotNull(icon.background, icon.foreground)) {
            part.setBounds(-inset, -inset, size + inset, size + inset)
            part.draw(canvas)
        }
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        canvas.drawPath(path(shape, s), mask)
        canvas.restoreToCount(layer)
    }

    fun path(shape: IconShape, s: Float): Path = Path().apply {
        val box = RectF(0f, 0f, s, s)
        when (shape) {
            IconShape.CIRCLE -> addOval(box, Path.Direction.CW)
            IconShape.ROUNDED_SQUARE -> addRoundRect(box, s * ROUNDED_RADIUS, s * ROUNDED_RADIUS, Path.Direction.CW)
            IconShape.TEARDROP -> {
                val r = s / 2
                val tip = s * ROUNDED_RADIUS / 2
                addRoundRect(box, floatArrayOf(r, r, r, r, r, r, tip, tip), Path.Direction.CW)
            }
            IconShape.SQUIRCLE, IconShape.SYSTEM -> squircle(this, s)
        }
    }

    private fun squircle(path: Path, s: Float) {
        val c = s / 2
        for (i in 0..SQUIRCLE_STEPS) {
            val t = 2 * PI * i / SQUIRCLE_STEPS
            val x = c + c * sign(cos(t)).toFloat() * abs(cos(t)).pow(SQUIRCLE_EXPONENT).toFloat()
            val y = c + c * sign(sin(t)).toFloat() * abs(sin(t)).pow(SQUIRCLE_EXPONENT).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}
