package dev.nova.core.domain.search

/** Telas de Configurações pesquisáveis (ações públicas do Android, funcionam no One UI). */
object SettingsCatalog {
    /** Entrada do catálogo. */
    class Item(val action: String, val label: String, keywords: String) {
        internal val entry: SearchEntry = FuzzyMatcher.entryFor("$label $keywords")
    }

    val items: List<Item> = listOf(
        Item("android.settings.WIFI_SETTINGS", "Wi-Fi", "internet rede wireless"),
        Item("android.settings.BLUETOOTH_SETTINGS", "Bluetooth", "fone pareamento"),
        Item("android.settings.DISPLAY_SETTINGS", "Tela", "brilho display fonte modo escuro taxa atualização"),
        Item("android.settings.SOUND_SETTINGS", "Som", "volume toque vibração audio"),
        Item("android.settings.BATTERY_SAVER_SETTINGS", "Economia de bateria", "energia"),
        Item("android.settings.LOCATION_SOURCE_SETTINGS", "Localização", "gps"),
        Item("android.settings.APPLICATION_SETTINGS", "Aplicativos", "apps permissões"),
        Item("android.settings.SECURITY_SETTINGS", "Segurança", "senha biometria bloqueio"),
        Item("android.settings.DATE_SETTINGS", "Data e hora", "fuso relógio"),
        Item("android.settings.LOCALE_SETTINGS", "Idioma", "língua region"),
        Item("android.settings.NFC_SETTINGS", "NFC", "pagamento aproximação"),
        Item("android.settings.AIRPLANE_MODE_SETTINGS", "Modo avião", "offline"),
        Item("android.settings.WIRELESS_SETTINGS", "Conexões", "rede móvel dados"),
        Item("android.settings.INTERNAL_STORAGE_SETTINGS", "Armazenamento", "espaço memória"),
        Item("android.settings.ACCESSIBILITY_SETTINGS", "Acessibilidade", "talkback"),
        Item("android.settings.SETTINGS", "Configurações", "ajustes"),
    )
}
