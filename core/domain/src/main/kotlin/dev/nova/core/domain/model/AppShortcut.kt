package dev.nova.core.domain.model

import androidx.compose.runtime.Immutable

/** Atalho de app (long-press): dinâmico, estático (manifest) ou fixado. */
@Immutable
data class AppShortcut(val id: String, val packageName: String, val label: String, val userSerial: Long)
