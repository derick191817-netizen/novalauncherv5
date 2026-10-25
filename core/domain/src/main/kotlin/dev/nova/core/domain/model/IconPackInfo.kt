package dev.nova.core.domain.model

import androidx.compose.runtime.Immutable

/** Pacote de ícones instalado (padrão ADW/Nova `appfilter.xml`). */
@Immutable
data class IconPackInfo(val packageName: String, val label: String)
