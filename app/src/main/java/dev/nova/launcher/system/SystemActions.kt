package dev.nova.launcher.system

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.domain.model.LockMethod
import dev.nova.launcher.access.LockAccessibilityService
import dev.nova.launcher.admin.LockDeviceAdminReceiver
import javax.inject.Inject
import javax.inject.Singleton

/** Ações de sistema usadas pelos gestos: bloquear a tela e abrir notificações. */
@Singleton
class SystemActions @Inject constructor(@ApplicationContext private val context: Context) {
    private val admin = ComponentName(context, LockDeviceAdminReceiver::class.java)

    /** Tenta o método escolhido e, se falhar, o outro. @return false se nenhum estiver ativo. */
    fun lockScreen(method: LockMethod): Boolean = when (method) {
        LockMethod.NONE -> false
        LockMethod.DEVICE_ADMIN -> lockWithAdmin() || LockAccessibilityService.lock()
        LockMethod.ACCESSIBILITY -> LockAccessibilityService.lock() || lockWithAdmin()
    }

    /** Abre o painel de notificações (EXPAND_STATUS_BAR), com fallback para acessibilidade. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(): Boolean {
        try {
            val statusBar = context.getSystemService("statusbar")
            if (statusBar != null) {
                statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
                return true
            }
        } catch (_: ReflectiveOperationException) {
            // método oculto indisponível nesta versão: usa o fallback abaixo
        } catch (_: SecurityException) {
            // sem EXPAND_STATUS_BAR
        }
        return LockAccessibilityService.openNotifications()
    }

    /** Intent do diálogo oficial para ativar o Device Admin. */
    fun deviceAdminIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Usado apenas para bloquear a tela com toque duplo.")

    private fun lockWithAdmin(): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        if (!dpm.isAdminActive(admin)) return false
        dpm.lockNow()
        return true
    }
}
