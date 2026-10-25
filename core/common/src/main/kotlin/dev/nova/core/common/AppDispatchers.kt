package dev.nova.core.common

import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Dispatchers injetáveis: permite trocar por dispatchers de teste e evita `Dispatchers.X` espalhado. */
interface AppDispatchers {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

/** Implementação de produção. */
class DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main.immediate
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
}

/** Escopo do processo (SupervisorJob + Default). Usar com parcimônia: nada de trabalho permanente. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
