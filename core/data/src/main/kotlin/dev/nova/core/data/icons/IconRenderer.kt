package dev.nova.core.data.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.UserManager
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.IconShape

/** Desenha o ícone (icon pack > adaptativo > legado) do app em um bitmap [sizePx] x [sizePx]. */
internal class IconRenderer(
    private val context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val packLoader: IconPackLoader,
) {
    /** Operação pesada: chamar apenas em dispatcher de I/O. */
    fun render(
        app: AppInfo,
        sizePx: Int,
        packPackage: String,
        themed: Boolean = false,
        night: Boolean = false,
        shape: IconShape = IconShape.SYSTEM,
    ): Bitmap? {
        val user = userManager.getUserForSerialNumber(app.userSerial) ?: return null
        val density = context.resources.displayMetrics.densityDpi
        var drawable = packLoader.iconFor(packPackage, ComponentName(app.packageName, app.className), density)
        if (drawable == null) {
            val activity = launcherApps.getActivityList(app.packageName, user)
                .firstOrNull { it.name == app.className } ?: return null
            val base = activity.getIcon(density) ?: return null
            drawable = if (themed) ThemedIconProvider.create(context, base, night) ?: base else base
            if (app.isWorkProfile) drawable = context.packageManager.getUserBadgedIcon(drawable, user)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (shape != IconShape.SYSTEM && drawable is AdaptiveIconDrawable) {
            IconShapes.drawMasked(canvas, drawable, sizePx, shape)
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        }
        return bitmap
    }
}
