package dev.nova.launcher

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import dev.nova.core.domain.repository.IconRepository
import javax.inject.Inject

/**
 * Application. Mantida mínima de propósito: tudo que é caro é preguiçoso (Hilt `Lazy`), o que
 * mantém o cold start curto. Nenhum serviço ou wakelock permanente.
 */
@HiltAndroidApp
class NovaApp : Application() {
    @Inject lateinit var icons: dagger.Lazy<IconRepository>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Em debug, qualquer I/O ou trabalho pesado na main thread aparece no logcat.
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        icons.get().onTrimMemory(level)
    }
}
