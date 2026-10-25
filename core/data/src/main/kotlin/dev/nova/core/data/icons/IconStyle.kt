package dev.nova.core.data.icons

import dev.nova.core.common.toEnumOrDefault
import dev.nova.core.domain.model.IconShape

/** Tudo que muda a aparência do ícone; vira parte da chave de cache (mudou o estilo => ícones refeitos). */
internal data class IconStyle(val pack: String, val themed: Boolean, val shape: IconShape) {
    val key: String get() = "$pack|${if (themed) 1 else 0}|${shape.name}"

    companion object {
        fun parse(key: String): IconStyle {
            val parts = key.split('|')
            return IconStyle(
                pack = parts.getOrElse(0) { "" },
                themed = parts.getOrNull(1) == "1",
                shape = parts.getOrNull(2).toEnumOrDefault(IconShape.SYSTEM),
            )
        }
    }
}
