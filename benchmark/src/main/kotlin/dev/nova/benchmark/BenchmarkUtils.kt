package dev.nova.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

internal const val PKG = "dev.nova.launcher"

/** Abre o launcher pela Activity HOME. Pré-requisito: Nova definido como launcher padrão (ver README). */
internal fun MacrobenchmarkScope.startLauncher() {
    pressHome()
    startActivityAndWait(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(ComponentName(PKG, "$PKG.LauncherActivity")),
    )
}

/** Swipe para cima abre a gaveta; swipes seguintes rolam a grade (gestos por coordenada: sem test tags). */
internal fun MacrobenchmarkScope.openDrawerAndScroll(flings: Int = 4) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val w = device.displayWidth
    val h = device.displayHeight
    device.swipe(w / 2, (h * 0.8).toInt(), w / 2, (h * 0.2).toInt(), 20)
    device.waitForIdle()
    repeat(flings) {
        device.swipe(w / 2, (h * 0.8).toInt(), w / 2, (h * 0.25).toInt(), 15)
        device.waitForIdle()
    }
}
