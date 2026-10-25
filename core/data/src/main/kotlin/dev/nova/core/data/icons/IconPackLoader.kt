package dev.nova.core.data.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Lê icon packs no formato aberto ADW/Nova (`appfilter.xml`: componente → drawable).
 * Não existe "IconPack API oficial" do Android; este é o padrão de fato suportado por centenas de packs.
 * Mantém só o último pack carregado em memória.
 */
internal class IconPackLoader(private val context: Context) {
    private class Pack(val packageName: String, val resources: Resources, val map: Map<ComponentName, String>)

    @Volatile
    private var loaded: Pack? = null

    /** Drawable do pack para [component], ou null (usa o ícone do sistema). Chamar em I/O. */
    fun iconFor(packPackage: String, component: ComponentName, densityDpi: Int): Drawable? {
        if (packPackage.isEmpty()) return null
        val pack = packFor(packPackage) ?: return null
        val name = pack.map[component] ?: return null
        val id = pack.resources.getIdentifier(name, "drawable", packPackage)
        if (id == 0) return null
        return try {
            pack.resources.getDrawableForDensity(id, densityDpi, null)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    @Synchronized
    private fun packFor(pkg: String): Pack? {
        loaded?.takeIf { it.packageName == pkg }?.let { return it }
        val parsed = parse(pkg)
        loaded = parsed
        return parsed
    }

    private fun parse(pkg: String): Pack? {
        try {
            val res = context.packageManager.getResourcesForApplication(pkg)
            val xmlId = res.getIdentifier("appfilter", "xml", pkg)
            val parser: XmlPullParser = if (xmlId != 0) {
                res.getXml(xmlId)
            } else {
                XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(context.createPackageContext(pkg, 0).assets.open("appfilter.xml"), null)
                }
            }
            val map = HashMap<ComponentName, String>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (comp != null && drawable != null) parseComponent(comp)?.let { map[it] = drawable }
                }
                event = parser.next()
            }
            return Pack(pkg, res, map)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        } catch (_: XmlPullParserException) {
            return null
        } catch (_: IOException) {
            return null
        }
    }

    // "ComponentInfo{pkg/cls}" -> ComponentName
    private fun parseComponent(raw: String): ComponentName? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) ComponentName.unflattenFromString(raw.substring(start + 1, end)) else null
    }
}
