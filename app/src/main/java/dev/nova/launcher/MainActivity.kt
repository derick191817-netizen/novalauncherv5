package dev.nova.launcher

import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import dev.nova.feature.settings.SettingsActions
import dev.nova.feature.settings.SettingsRoute
import dev.nova.launcher.system.SystemActions
import javax.inject.Inject

/** Tela de configurações (ícone "Nova Launcher" na gaveta de qualquer launcher). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var system: SystemActions

    // O diálogo do sistema decide sozinho; não há nada a fazer com o resultado além de voltar para esta tela.
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsRoute(
                actions = SettingsActions(
                    requestDeviceAdmin = { open(system.deviceAdminIntent()) },
                    openAccessibility = { open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    pickWallpaper = { open(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Escolher wallpaper")) },
                    pickLiveWallpaper = { open(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) },
                    openDefaultHome = ::requestHomeRole,
                ),
            )
        }
    }

    /** Android 10+: abre direto o diálogo "definir como app inicial"; se indisponível, a tela de configurações. */
    private fun requestHomeRole() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            open(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // tela indisponível neste aparelho
        }
    }
}
