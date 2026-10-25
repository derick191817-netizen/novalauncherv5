package dev.nova.feature.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Widget instalado disponível para o usuário adicionar. */
@Immutable
data class WidgetProviderUi(
    val component: ComponentName,
    val label: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
) {
    /** Colunas ocupadas (fórmula padrão do Android: (dp + 30) / 70). */
    val spanX: Int get() = ((minWidthDp + 30) / CELL_DP).coerceAtLeast(1)

    /** Linhas ocupadas. */
    val spanY: Int get() = ((minHeightDp + 30) / CELL_DP).coerceAtLeast(1)

    private companion object {
        const val CELL_DP = 70
    }
}

/**
 * Envolve [AppWidgetHost]. O host só "escuta" atualizações enquanto a UI está visível
 * (startListening/stopListening ligados ao ciclo de vida): nenhum trabalho em background.
 *
 * O bind usa `bindAppWidgetIdIfAllowed`; se o sistema negar, o fluxo cai no diálogo oficial
 * ACTION_APPWIDGET_BIND (BIND_APPWIDGET é permissão de sistema e não é declarada).
 */
@Singleton
class WidgetController @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, HOST_ID)

    @Volatile
    private var listening = false

    fun startListening() {
        if (!listening) {
            host.startListening()
            listening = true
        }
    }

    fun stopListening() {
        if (listening) {
            host.stopListening()
            listening = false
        }
    }

    /** Lista widgets instalados (consulta pesada: chamar fora da main). */
    fun providers(): List<WidgetProviderUi> {
        val density = context.resources.displayMetrics.density
        val pm = context.packageManager
        return manager.installedProviders
            .map {
                WidgetProviderUi(
                    component = it.provider,
                    label = it.loadLabel(pm).orEmpty(),
                    minWidthDp = (it.minWidth / density).toInt(),
                    minHeightDp = (it.minHeight / density).toInt(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun delete(appWidgetId: Int) = host.deleteAppWidgetId(appWidgetId)

    fun bindIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
        manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)

    /** Intent do diálogo do sistema pedindo permissão para o launcher exibir o widget. */
    fun bindIntent(appWidgetId: Int, provider: ComponentName): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)

    fun needsConfigure(appWidgetId: Int): Boolean = manager.getAppWidgetInfo(appWidgetId)?.configure != null

    /** Abre a tela de configuração do widget (precisa de uma Activity para receber o resultado). */
    @Suppress("DEPRECATION")
    fun startConfigure(activity: android.app.Activity, appWidgetId: Int, requestCode: Int) =
        host.startAppWidgetConfigureActivityForResult(activity, appWidgetId, 0, requestCode, null)

    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(appWidgetId) ?: return null
        return host.createView(context, appWidgetId, info)
    }

    private companion object {
        const val HOST_ID = 1024
    }
}
