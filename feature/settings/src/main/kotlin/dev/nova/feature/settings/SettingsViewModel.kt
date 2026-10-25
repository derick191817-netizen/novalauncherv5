package dev.nova.feature.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.IconPackInfo
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.usecase.ExportBackupUseCase
import dev.nova.core.domain.usecase.ImportBackupUseCase
import dev.nova.core.domain.usecase.ListIconPacksUseCase
import dev.nova.core.domain.usecase.ObserveSettingsUseCase
import dev.nova.core.domain.usecase.UpdateSettingsUseCase
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Estado da tela de configurações.
 * [settings] é o RASCUNHO (o que o usuário edita e vê no preview); [persisted] é o que está salvo e vale para
 * o launcher. [hasChanges] liga a barra Salvar/Descartar.
 */
@Immutable
data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val iconPacks: List<IconPackInfo> = emptyList(),
    val persisted: LauncherSettings = LauncherSettings(),
    val hasChanges: Boolean = false,
)

/**
 * Preferências com rascunho: [update] só altera o rascunho; nada vale até [save]. [discard] volta ao salvo.
 * Também cuida de icon packs e backup/restore.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val listIconPacks: ListIconPacksUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val packs = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _message = MutableStateFlow<String?>(null)
    private val persisted = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), LauncherSettings())
    private val draft = MutableStateFlow<LauncherSettings?>(null)

    /** Mensagem única para exibir (Toast); chame [messageShown] depois. */
    val message: StateFlow<String?> = _message

    val uiState: StateFlow<SettingsUiState> = combine(persisted, draft, packs) { saved, edited, installed ->
        SettingsUiState(
            settings = edited ?: saved,
            iconPacks = installed,
            persisted = saved,
            hasChanges = edited != null && edited != saved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), SettingsUiState())

    init {
        viewModelScope.launch { packs.value = listIconPacks() }
    }

    /** Altera só o rascunho. O launcher continua com as configurações salvas até o usuário tocar em Salvar. */
    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        draft.update { current -> transform(current ?: persisted.value) }
    }

    /** Grava o rascunho. [onDone] roda quando a gravação terminou (ex.: para fechar a tela depois). */
    fun save(onDone: () -> Unit = {}) {
        val edited = draft.value
        if (edited == null) {
            onDone()
            return
        }
        viewModelScope.launch {
            // NonCancellable: se a tela fechar logo após o toque em Salvar, a gravação ainda termina.
            withContext(NonCancellable) {
                updateSettings { edited }
                withTimeoutOrNull(SAVE_WAIT_MS) { persisted.first { it == edited.sanitized() } }
                draft.value = null
            }
            _message.value = "Configurações salvas"
            onDone()
        }
    }

    /** Descarta o rascunho e volta ao que estava salvo. */
    fun discard() {
        draft.value = null
    }

    /** Põe os valores de fábrica no rascunho (continua sendo possível Descartar). */
    fun resetDraftToDefaults() {
        draft.value = LauncherSettings()
    }

    fun messageShown() { _message.update { null } }

    /** Grava o backup JSON em [uri] (escolhido pelo seletor de arquivos; pode ser o Google Drive). */
    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = try {
                val json = exportBackup()
                withContext(dispatchers.io) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
                }
            } catch (_: IOException) {
                false
            }
            _message.value = if (ok) "Backup salvo" else "Não foi possível salvar o backup"
        }
    }

    /** Restaura o backup JSON de [uri]; o rascunho em edição é descartado. */
    fun import(uri: Uri) {
        viewModelScope.launch {
            val json = try {
                withContext(dispatchers.io) { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
            } catch (_: IOException) {
                null
            }
            val ok = json != null && importBackup(json)
            if (ok) draft.value = null
            _message.value = if (ok) "Backup restaurado" else "Arquivo de backup inválido"
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
        const val SAVE_WAIT_MS = 2_000L
    }
}
