plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
