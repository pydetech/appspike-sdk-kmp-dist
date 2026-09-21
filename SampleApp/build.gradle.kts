// Root build file — plugin versions declared once here so sibling modules share
// one plugin classloader; subprojects apply them without versions.
plugins {
    id("com.android.application") version "9.0.0" apply false
    id("com.android.kotlin.multiplatform.library") version "9.0.0" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}
