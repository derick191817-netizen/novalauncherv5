package dev.nova.core.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.nova.core.common.toEnumOrDefault
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

private object K {
    val rows = intPreferencesKey("grid_rows")
    val columns = intPreferencesKey("grid_columns")
    val padding = intPreferencesKey("grid_padding_dp")
    val drawerColumns = intPreferencesKey("drawer_columns")
    val dockSize = intPreferencesKey("dock_size")
    val theme = stringPreferencesKey("theme")
    val performance = stringPreferencesKey("performance")
    val profile = stringPreferencesKey("profile")
    val swipeUp = stringPreferencesKey("gesture_swipe_up")
    val swipeDown = stringPreferencesKey("gesture_swipe_down")
    val doubleTap = stringPreferencesKey("gesture_double_tap")
    val pinch = stringPreferencesKey("gesture_pinch")
    val edgeSwipe = booleanPreferencesKey("gesture_edge_swipe")
    val smartFolders = booleanPreferencesKey("feat_smart_folders")
    val hiddenApps = booleanPreferencesKey("feat_hidden_apps")
    val widgetStack = booleanPreferencesKey("feat_widget_stack")
    val lockMethod = stringPreferencesKey("feat_lock_method")
    val searchContacts = booleanPreferencesKey("search_contacts")
    val searchSettings = booleanPreferencesKey("search_settings")
    val searchWeb = booleanPreferencesKey("search_web")
    val dragAndDrop = booleanPreferencesKey("feat_drag_and_drop")
    val themedIcons = booleanPreferencesKey("feat_themed_icons")
    val iconPack = stringPreferencesKey("icon_pack")
    val iconShape = stringPreferencesKey("icon_shape")
    val drawerSort = stringPreferencesKey("drawer_sort")
    val lockLayout = booleanPreferencesKey("lock_home_layout")
}

/** Lê as preferências; chaves ausentes ou inválidas caem nos valores padrão. */
internal fun Preferences.toSettings(): LauncherSettings {
    val d = LauncherSettings()
    return LauncherSettings(
        grid = GridSettings(
            rows = this[K.rows] ?: d.grid.rows,
            columns = this[K.columns] ?: d.grid.columns,
            paddingDp = this[K.padding] ?: d.grid.paddingDp,
            drawerColumns = this[K.drawerColumns] ?: d.grid.drawerColumns,
        ),
        dockSize = this[K.dockSize] ?: d.dockSize,
        theme = this[K.theme].toEnumOrDefault(d.theme),
        performance = this[K.performance].toEnumOrDefault(PerformanceMode.NORMAL),
        profile = this[K.profile].toEnumOrDefault(UsageProfile.PERSONAL),
        gestures = GestureSettings(
            swipeUp = this[K.swipeUp].toEnumOrDefault(d.gestures.swipeUp),
            swipeDown = this[K.swipeDown].toEnumOrDefault(d.gestures.swipeDown),
            doubleTap = this[K.doubleTap].toEnumOrDefault(d.gestures.doubleTap),
            pinch = this[K.pinch].toEnumOrDefault(d.gestures.pinch),
            edgeSwipeEnabled = this[K.edgeSwipe] ?: d.gestures.edgeSwipeEnabled,
        ),
        features = FeatureFlags(
            smartFolders = this[K.smartFolders] ?: false,
            hiddenApps = this[K.hiddenApps] ?: false,
            widgetStack = this[K.widgetStack] ?: false,
            lockMethod = this[K.lockMethod].toEnumOrDefault(LockMethod.NONE),
            searchContacts = this[K.searchContacts] ?: false,
            searchSettings = this[K.searchSettings] ?: false,
            searchWeb = this[K.searchWeb] ?: false,
            dragAndDrop = this[K.dragAndDrop] ?: false,
            themedIcons = this[K.themedIcons] ?: false,
        ),
        iconPackPackage = this[K.iconPack].orEmpty(),
        iconShape = this[K.iconShape].toEnumOrDefault(IconShape.SYSTEM),
        drawerSort = this[K.drawerSort].toEnumOrDefault(AppSort.ALPHABETICAL),
        lockHomeLayout = this[K.lockLayout] ?: false,
    ).sanitized()
}

/** Grava todas as preferências. */
internal fun MutablePreferences.write(s: LauncherSettings) {
    this[K.rows] = s.grid.rows
    this[K.columns] = s.grid.columns
    this[K.padding] = s.grid.paddingDp
    this[K.drawerColumns] = s.grid.drawerColumns
    this[K.dockSize] = s.dockSize
    this[K.theme] = s.theme.name
    this[K.performance] = s.performance.name
    this[K.profile] = s.profile.name
    this[K.swipeUp] = s.gestures.swipeUp.name
    this[K.swipeDown] = s.gestures.swipeDown.name
    this[K.doubleTap] = s.gestures.doubleTap.name
    this[K.pinch] = s.gestures.pinch.name
    this[K.edgeSwipe] = s.gestures.edgeSwipeEnabled
    this[K.smartFolders] = s.features.smartFolders
    this[K.hiddenApps] = s.features.hiddenApps
    this[K.widgetStack] = s.features.widgetStack
    this[K.lockMethod] = s.features.lockMethod.name
    this[K.searchContacts] = s.features.searchContacts
    this[K.searchSettings] = s.features.searchSettings
    this[K.searchWeb] = s.features.searchWeb
    this[K.dragAndDrop] = s.features.dragAndDrop
    this[K.themedIcons] = s.features.themedIcons
    this[K.iconPack] = s.iconPackPackage
    this[K.iconShape] = s.iconShape.name
    this[K.drawerSort] = s.drawerSort.name
    this[K.lockLayout] = s.lockHomeLayout
}

// Evita aviso de import não usado quando GestureAction só aparece via inferência.
private typealias Unused = GestureAction
