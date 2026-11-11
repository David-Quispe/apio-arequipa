import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Clave de MapTiler (teselas vectoriales del mapa). Vive en local.properties,
// que esta en .gitignore: nunca se sube al repositorio. Si falta, la app usa
// un estilo libre sin clave (ver ConfigMapa en :shared).
val claveMapTiler: String = Properties().apply {
    val archivo = rootDir.resolve("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}.getProperty("MAPTILER_KEY", "").trim()

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Red: Retrofit + OkHttp (los modelos @Serializable vienen de :shared)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinxSerialization)

    // Persistencia local: Room (cache de hospitales, ultima ruta e historial)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.apio.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.apio.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MAPTILER_KEY", "\"$claveMapTiler\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}