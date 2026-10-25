package dev.nova.core.data.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.nova.core.domain.model.AppSort
import dev.nova.core.domain.model.FeatureFlags
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.domain.model.GestureSettings
import dev.nova.core.domain.model.GridSettings
import dev.nova.core.domain.model.IconShape
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.model.LockMethod
import dev.nova.core.domain.model.PerformanceMode
import dev.nova.core.domain.model.ThemeMode
import dev.nova.core.domain.model.UsageProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsKeysTest {
    private val custom = LauncherSettings(
        grid = GridSettings(rows = 8, columns = 6, paddingDp = 20, drawerColumns = 4),
        dockSize = 7,
        theme = ThemeMode.AMOLED_BLACK,
        performance = PerformanceMode.BATTERY_SAVER,
        profile = UsageProfile.GAMING,
        gestures = GestureSettings(GestureAction.OPEN_SEARCH, GestureAction.NONE, GestureAction.OPEN_SETTINGS, GestureAction.LOCK_SCREEN, true),
        features = FeatureFlags(true, true, true, LockMethod.ACCESSIBILITY, true, true, true, dragAndDrop = true, themedIcons = true),
        iconPackPackage = "com.pack.x",
        iconShape = IconShape.TEARDROP,
        drawerSort = AppSort.RECENTLY_INSTALLED,
        lockHomeLayout = true,
    )

    @Test fun everyFieldSurvivesAWriteAndRead() {
        val prefs = mutablePreferencesOf()
        prefs.write(custom)
        assertEquals(custom, prefs.toSettings())
    }

    @Test fun emptyPreferencesGiveDefaults() = assertEquals(LauncherSettings(), mutablePreferencesOf().toSettings())

    @Test fun invalidEnumValuesFallBackToDefaults() {
        val prefs = mutablePreferencesOf()
        prefs[stringPreferencesKey("theme")] = "XYZ"
        prefs[stringPreferencesKey("icon_shape")] = "???"
        val s = prefs.toSettings()
        assertEquals(ThemeMode.AUTO, s.theme)
        assertEquals(IconShape.SYSTEM, s.iconShape)
    }
}
