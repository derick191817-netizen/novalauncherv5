package dev.nova.launcher.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.nova.feature.widgets.WidgetController
import javax.inject.Inject

/**
 * Activity transparente que abre a tela de configuração de um widget e devolve o resultado.
 * (`startAppWidgetConfigureActivityForResult` só entrega o resultado via onActivityResult.)
 */
@AndroidEntryPoint
class WidgetConfigHostActivity : ComponentActivity() {
    @Inject lateinit var widgets: WidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        if (savedInstanceState == null) widgets.startConfigure(this, id, REQUEST_CONFIGURE)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE) {
            setResult(resultCode, data)
            finish()
        }
    }

    private companion object {
        const val REQUEST_CONFIGURE = 42
    }
}
