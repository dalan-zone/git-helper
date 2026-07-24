import java.util.Properties
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // versionName: human-edited SemVer from gradle.properties
        versionName = (project.findProperty("VERSION_NAME") as? String) ?: "1.0.0"
        // versionCode: auto-derived (see computeVersionCode below)
        versionCode = computeVersionCode()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // ---- Signing ----
    // Local: put a release.keystore at app/release.keystore and set secrets in ~/.gradle/gradle.properties
    // CI: provide KEYSTORE_FILE (path) + KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD as env vars.
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: rootProject.file("app/release.keystore").path)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    // ---- Build types ----
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing: configured in signingConfigs below.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ---- Product flavors: one dimension "environment" ----
    // Demonstrates multi-environment builds driven by the catalog + buildConfig.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com\"")
            manifestPlaceholders["appLabel"] = "App Dev"
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "BASE_URL", "\"https://staging.api.example.com\"")
            manifestPlaceholders["appLabel"] = "App Staging"
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.example.com\"")
            manifestPlaceholders["appLabel"] = "App"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ---------------------------------------------------------------------------
// Auto versionCode resolver (priority order):
//   1. CI-provided env VERSION_CODE  (monotonic, committed back by the workflow)
//   2. version.properties VERSION_CODE (local fallback, also bumped by CI)
//   3. git commit count               (so local builds always get a unique number)
// ---------------------------------------------------------------------------
fun computeVersionCode(): Int {
    val envCode = System.getenv("VERSION_CODE")?.toIntOrNull()
    if (envCode != null) return envCode

    val versionPropsFile = rootProject.file("version.properties")
    if (versionPropsFile.exists()) {
        val props = Properties()
        versionPropsFile.inputStream().use { props.load(it) }
        val propCode = props.getProperty("VERSION_CODE")?.toIntOrNull()
        if (propCode != null) return propCode
    }

    return try {
        val out = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = out
        }
        out.toString().trim().toInt()
    } catch (e: Exception) {
        1
    }
}
