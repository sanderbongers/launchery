plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
}

// Pruning build directories while walking the tree keeps Spotless from reading files that a parallel task deletes.
fun sources(pattern: String) =
    fileTree(rootDir) {
        include(pattern)
        exclude("**/build/**", "**/.gradle/**")
    }

spotless {
    kotlin {
        target(sources("**/*.kt"))
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(sources("**/*.gradle.kts"))
        ktlint(libs.versions.ktlint.get())
    }
}
