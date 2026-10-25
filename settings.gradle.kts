pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "NovaLauncher"
include(
    ":app",
    ":core:ui", ":core:data", ":core:domain", ":core:common",
    ":feature:home", ":feature:drawer", ":feature:settings", ":feature:widgets", ":feature:gestures",
    // :benchmark entra só com -PwithBenchmark (ver abaixo)
)

// Macrobenchmark + Baseline Profile exigem aparelho real e são a parte mais frágil do build. O CI não os usa,
// então ficam FORA por padrão e nunca derrubam o APK. Para ligar: gradle -PwithBenchmark ...
if (providers.gradleProperty("withBenchmark").isPresent) include(":benchmark")
