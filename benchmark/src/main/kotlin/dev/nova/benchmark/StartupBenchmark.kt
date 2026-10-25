package dev.nova.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Meta: cold start < 400ms (timeToInitialDisplayMs). Compare `None` x `Partial` (com Baseline Profile). */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun coldStartNoCompilation() = run(CompilationMode.None())

    @Test fun coldStartWithBaselineProfile() = run(CompilationMode.Partial())

    private fun run(mode: CompilationMode) = rule.measureRepeated(
        packageName = PKG,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = mode,
    ) { startLauncher() }
}
