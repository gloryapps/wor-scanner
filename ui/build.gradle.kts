plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

/* What the build knows and the code reads: a multiplatform module has no BuildConfig. */
val built by tasks.registering {
    val source = layout.buildDirectory.dir("generated/built")
    val appName = providers.gradleProperty("worscanner.name")
    val lab = providers.gradleProperty("azhor.url")
    inputs.property("name", appName)
    inputs.property("lab", lab)
    outputs.dir(source)
    doLast {
        source.get().file("com/gloryapps/worscanner/ui/Built.kt").asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                |package com.gloryapps.worscanner.ui
                |
                |/** Written by the build from `gradle.properties`. */
                |object Built {
                |    const val NAME = "${appName.get()}"
                |    const val AZHOR_URL = "${lab.get()}"
                |}
                |""".trimMargin(),
            )
        }
    }
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.gloryapps.worscanner.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        /* Packs the strings below into the APK, where Android reads them at run time. */
        androidResources {
            enable = true
        }
    }
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(built)
            dependencies {
                api(project(":scanner"))
                api(libs.multiplatform.runtime)
                api(libs.multiplatform.foundation)
                api(libs.multiplatform.ui)
                api(libs.multiplatform.material3)
                api(libs.multiplatform.iconsCore)
                api(libs.multiplatform.resources)
            }
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            /* Resources ask the system's theme through skiko, whose native part only an app brings. */
            implementation(compose.desktop.currentOs)
        }
    }
}

/* Public: the apps say what `ui` holds, as Cancel, through the same `Res`. */
compose.resources {
    packageOfResClass = "com.gloryapps.worscanner.ui.resources"
    publicResClass = true
}
