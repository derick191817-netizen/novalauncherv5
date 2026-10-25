package dev.nova.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Gera o Baseline Profile percorrendo os caminhos críticos: start, home, gaveta, rolagem. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun generate() = rule.collect(packageName = PKG, includeInStartupProfile = true) {
        startLauncher()
        openDrawerAndScroll(flings = 3)
        device.pressBack()
    }
}
