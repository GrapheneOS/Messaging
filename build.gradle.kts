plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.6")
    }
}
