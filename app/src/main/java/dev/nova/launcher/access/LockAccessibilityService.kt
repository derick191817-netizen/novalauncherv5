package dev.nova.launcher.access

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço de acessibilidade mínimo: só executa ações globais (bloquear tela, abrir notificações).
 * Não escuta eventos nem lê a tela, e só existe se o usuário o ativar manualmente.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        private var instance: LockAccessibilityService? = null

        /** Bloqueia a tela mantendo a biometria (GLOBAL_ACTION_LOCK_SCREEN, API 28+). */
        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false

        /** Abre o painel de notificações. */
        fun openNotifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }
}
