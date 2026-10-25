package dev.nova.core.data.hidden

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.repository.HiddenAppsRepository
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * Apps ocultos em [EncryptedSharedPreferences] (AES-256, chave no Android Keystore).
 * A criação do MasterKey é cara: acontece de forma preguiçosa, em I/O, só quando o recurso é usado.
 */
@Singleton
class HiddenAppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : HiddenAppsRepository {

    private val state = MutableStateFlow<Set<String>?>(null)

    private val prefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: GeneralSecurityException) {
            null // Keystore indisponível/corrompido: o recurso fica desativado em vez de travar o launcher.
        } catch (_: IOException) {
            null
        }
    }

    override val hiddenKeys: Flow<Set<String>> = state.onStart { ensureLoaded() }.filterNotNull()

    override suspend fun setHidden(appKey: String, hidden: Boolean) {
        ensureLoaded()
        val current = state.value.orEmpty()
        val next = if (hidden) current + appKey else current - appKey
        state.value = next
        withContext(dispatchers.io) { prefs?.edit()?.putStringSet(KEY, next)?.apply() }
    }

    private suspend fun ensureLoaded() {
        if (state.value != null) return
        val loaded = withContext(dispatchers.io) {
            prefs?.getStringSet(KEY, emptySet()).orEmpty().toSet()
        }
        state.compareAndSet(null, loaded)
    }

    private companion object {
        const val FILE = "hidden_apps"
        const val KEY = "keys"
    }
}
