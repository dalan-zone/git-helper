// Root build file: only declares plugins via the version catalog.
// Versions live in gradle/libs.versions.toml so there is a single source of truth.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
