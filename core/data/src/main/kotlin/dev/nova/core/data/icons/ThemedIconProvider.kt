package dev.nova.core.data.icons

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Ícones temáticos (Material You): usa a camada monocromática (Android 13+) do ícone adaptativo, pintada com as
 * cores de destaque do sistema (derivadas do wallpaper). Devolve null se o app não tiver camada monocromática
 * (o chamador usa o ícone normal), ou se o Android for anterior ao 13.
 */
internal object ThemedIconProvider {
    fun create(context: Context, source: Drawable, night: Boolean): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || source !is AdaptiveIconDrawable) return null
        val mono = source.monochrome ?: return null
        val background = context.getColor(if (night) android.R.color.system_accent1_800 else android.R.color.system_accent1_100)
        val tint = context.getColor(if (night) android.R.color.system_accent1_100 else android.R.color.system_accent1_800)
        val foreground = mono.mutate().apply { setTint(tint) }
        return AdaptiveIconDrawable(ColorDrawable(background), foreground)
    }
}
