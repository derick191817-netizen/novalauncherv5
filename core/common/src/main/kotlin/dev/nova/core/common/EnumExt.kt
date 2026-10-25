package dev.nova.core.common

/** Converte um nome salvo em disco para enum, caindo no [default] se o valor for inválido/antigo. */
inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
