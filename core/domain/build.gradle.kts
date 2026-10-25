plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.nova.core.domain"
    compileSdk = 34
    defaultConfig { minSdk = 29; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:common"))
    implementation("javax.inject:javax.inject:1")
    // @Immutable/@Stable vêm do runtime do Compose. A versão sai do BOM. O BOM é `api` de propósito: quem consome
    // :core:domain (data, ui, features) precisa dele para resolver o runtime, senão a versão fica vazia.
    api(platform(libs.compose.bom))
    api(libs.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
