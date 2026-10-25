package dev.nova.core.data.di

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.nova.core.common.AppDispatchers
import dev.nova.core.common.ApplicationScope
import dev.nova.core.common.DefaultAppDispatchers
import dev.nova.core.data.apps.AppRepositoryImpl
import dev.nova.core.data.backup.BackupRepositoryImpl
import dev.nova.core.data.contacts.ContactsRepositoryImpl
import dev.nova.core.data.icons.IconPackRepositoryImpl
import dev.nova.core.data.layout.HomeLayoutRepositoryImpl
import dev.nova.core.data.hidden.HiddenAppsRepositoryImpl
import dev.nova.core.data.icons.IconRepositoryImpl
import dev.nova.core.data.settings.SettingsRepositoryImpl
import dev.nova.core.domain.repository.AppRepository
import dev.nova.core.domain.repository.BackupRepository
import dev.nova.core.domain.repository.ContactsRepository
import dev.nova.core.domain.repository.HomeLayoutRepository
import dev.nova.core.domain.repository.IconPackRepository
import dev.nova.core.domain.repository.HiddenAppsRepository
import dev.nova.core.domain.repository.IconRepository
import dev.nova.core.domain.repository.SettingsRepository
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

/** Vincula interfaces do domínio às implementações. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {
    @Binds @Singleton abstract fun apps(impl: AppRepositoryImpl): AppRepository
    @Binds @Singleton abstract fun icons(impl: IconRepositoryImpl): IconRepository
    @Binds @Singleton abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun hidden(impl: HiddenAppsRepositoryImpl): HiddenAppsRepository
    @Binds @Singleton abstract fun layouts(impl: HomeLayoutRepositoryImpl): HomeLayoutRepository
    @Binds @Singleton abstract fun backup(impl: BackupRepositoryImpl): BackupRepository
    @Binds @Singleton abstract fun contacts(impl: ContactsRepositoryImpl): ContactsRepository
    @Binds @Singleton abstract fun iconPacks(impl: IconPackRepositoryImpl): IconPackRepository
}

/** Fornece dependências do sistema. */
@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {
    @Provides @Singleton
    fun dispatchers(): AppDispatchers = DefaultAppDispatchers()

    @Provides @Singleton @ApplicationScope
    fun appScope(dispatchers: AppDispatchers): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    @Provides @Singleton
    fun launcherApps(@ApplicationContext context: Context): LauncherApps =
        requireNotNull(context.getSystemService(LauncherApps::class.java)) { "LauncherApps indisponível" }

    @Provides @Singleton
    fun userManager(@ApplicationContext context: Context): UserManager =
        requireNotNull(context.getSystemService(UserManager::class.java)) { "UserManager indisponível" }

    @Provides @Singleton
    fun settingsStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
}
