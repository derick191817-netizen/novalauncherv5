package dev.nova.launcher

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.model.PerformanceMode
import dev.nova.core.domain.repository.IconRepository
import dev.nova.feature.drawer.DrawerViewModel
import dev.nova.feature.home.HomeViewModel
import dev.nova.feature.widgets.WidgetController
import dev.nova.feature.widgets.WidgetProviderUi
import dev.nova.launcher.system.HiddenAppsAuth
import dev.nova.launcher.system.SystemActions
import dev.nova.launcher.widgets.WidgetConfigHostActivity
import javax.inject.Inject

/**
 * Activity HOME. `singleTask` + `stateNotNeeded` (Manifest): uma única instância que o sistema pode
 * recriar sem salvar estado. Janela transparente sobre o wallpaper => sem fundo próprio (zero overdraw).
 */
@AndroidEntryPoint
class LauncherActivity : FragmentActivity() {
    @Inject lateinit var widgets: WidgetController
    @Inject lateinit var system: SystemActions
    @Inject lateinit var icons: IconRepository

    private val homeVm: HomeViewModel by viewModels()
    private val drawerVm: DrawerViewModel by viewModels()
    private var homeSignal by mutableIntStateOf(0)

    private var pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: WidgetProviderUi? = null

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) afterBind() else cancelPending()
    }
    private val configLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) finishWidget() else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(0), navigationBarStyle = SystemBarStyle.dark(0))
        setContent {
            LauncherRoot(
                homeVm = homeVm,
                drawerVm = drawerVm,
                widgets = widgets,
                icons = icons,
                homeSignal = homeSignal,
                onSystemAction = ::runSystemAction,
                onAddWidget = ::startAddWidget,
                onUnlockHidden = { onSuccess -> HiddenAppsAuth.authenticate(this, onSuccess) },
                onOpenSettings = { startActivity(Intent(this, MainActivity::class.java)) },
                onWindowEffects = ::applyWindowEffects,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) homeSignal++
    }

    private fun runSystemAction(action: GestureAction, settings: LauncherSettings) {
        when (action) {
            GestureAction.LOCK_SCREEN -> if (!system.lockScreen(settings.features.lockMethod)) {
                toast("Ative o bloqueio por toque duplo em Configurações")
            }
            GestureAction.OPEN_NOTIFICATIONS -> system.expandNotifications()
            else -> Unit
        }
    }

    // ---- fluxo de adicionar widget: alocar id -> bind (com diálogo se preciso) -> configurar -> colocar na home ----
    private fun startAddWidget(provider: WidgetProviderUi) {
        cancelPending()
        pendingId = widgets.allocateId()
        pendingProvider = provider
        if (widgets.bindIfAllowed(pendingId, provider.component)) afterBind() else bindLauncher.launch(widgets.bindIntent(pendingId, provider.component))
    }

    private fun afterBind() {
        if (widgets.needsConfigure(pendingId)) {
            configLauncher.launch(Intent(this, WidgetConfigHostActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId))
        } else {
            finishWidget()
        }
    }

    private fun finishWidget() {
        val provider = pendingProvider
        if (provider != null && pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            homeVm.addWidget(pendingId, provider.spanX, provider.spanY)
        }
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    private fun cancelPending() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) widgets.delete(pendingId)
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    // ---- efeitos de janela: taxa de atualização e blur ----
    private fun applyWindowEffects(mode: PerformanceMode, drawerOpen: Boolean) {
        applyRefreshRate(mode.maxRefreshHz)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Wallpaper é outra janela: o blur certo é o "blur behind" (flag + raio), não só o do fundo da janela.
            val radius = if (drawerOpen && mode.blur) BLUR_RADIUS_PX else 0
            window.setBackgroundBlurRadius(radius)
            if (radius > 0) window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND) else window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius }
        }
    }

    /**
     * Pede ao sistema o modo de tela de maior taxa na resolução atual (ou limitado a [maxHz]).
     * É uma preferência: o One UI continua dono do modo adaptativo de 10–120Hz.
     */
    private fun applyRefreshRate(maxHz: Int?) {
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter {
                it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight &&
                    (maxHz == null || it.refreshRate <= maxHz + 0.5f)
            }
            .maxByOrNull { it.refreshRate } ?: return
        if (window.attributes.preferredDisplayModeId != best.modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private companion object {
        const val BLUR_RADIUS_PX = 80
    }
}
