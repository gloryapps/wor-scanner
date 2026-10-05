plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":scanner"))
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.multiplatform.resources)
    implementation(libs.multiplatform.lifecycle.viewmodelCompose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.jna.platform)

    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "com.gloryapps.worscanner.windows.MainKt"
        /* The runtime shipped beside the app is cut from the JDK the module is compiled with. */
        javaHome = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }.get().metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            packageName = providers.gradleProperty("worscanner.name").get()
            packageVersion = providers.gradleProperty("worscanner.version").get()
            /* What `suggestRuntimeModules` found the app needs beyond Compose's own. */
            modules("java.instrument", "jdk.unsupported")
        }
    }
}

compose.resources {
    packageOfResClass = "com.gloryapps.worscanner.windows.resources"
}
