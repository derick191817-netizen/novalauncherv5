plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.nova.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.nova.launcher"
        minSdk = 29
        targetSdk = 34
        versionCode = 5
        versionName = "0.3.2-dev"
        // Só recursos de idiomas usados: reduz APK e memória de Resources.
        resourceConfigurations += listOf("en", "pt") // AGP 8.5: localeFilters só existe a partir do 8.8
    }

    signingConfigs {
        create("release") {
            // Lidos de ~/.gradle/gradle.properties ou variáveis de ambiente (nunca commitar).
            storeFile = (findProperty("NOVA_STORE_FILE") as String?)?.let(::file)
            storePassword = findProperty("NOVA_STORE_PASSWORD") as String?
            keyAlias = findProperty("NOVA_KEY_ALIAS") as String?
            keyPassword = findProperty("NOVA_KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true        // R8 full mode (gradle.properties)
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sem keystore (NOVA_STORE_FILE ausente) o APK sai SEM assinatura, em vez de quebrar a configuração.
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}


dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:drawer"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:gestures"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.profileinstaller)   // instala o Baseline Profile no 1º boot
    implementation(libs.biometric)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.coroutines.android)
    implementation("androidx.fragment:fragment-ktx:1.8.3") // BiometricPrompt exige FragmentActivity
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}

// Baseline Profile: só quando o módulo :benchmark está incluído (gradle -PwithBenchmark ...).
if (rootProject.findProject(":benchmark") != null) {
    apply(plugin = "androidx.baselineprofile")
    dependencies { add("baselineProfile", project(":benchmark")) }
}
