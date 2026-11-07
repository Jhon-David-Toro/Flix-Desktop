import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.components.resources)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.newpipe.extractor)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.datastore.preferences.core)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(compose.desktop.uiTestJUnit4)
}

tasks.test {
    systemProperty("flix.liveTests", providers.gradleProperty("liveTests").getOrElse("false"))
    systemProperty("flix.exportIcon", providers.gradleProperty("exportIcon").getOrElse("false"))
    systemProperty("compose.application.resources.dir", layout.projectDirectory.dir("resources/windows").asFile.absolutePath)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "com.flix.music.resources"
    publicResClass = false
}

compose.desktop {
    application {
        mainClass = "com.flix.music.MainKt"
        nativeDistributions.appResourcesRootDir = layout.projectDirectory.dir("resources")
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Flix"
            packageVersion = "1.1.2"
            description = "Flix Music para escritorio"
            vendor = "Jhon David Toro"
            copyright = "© 2026 Jhon David Toro. GPL-3.0."
            licenseFile.set(rootProject.file("LICENSE"))
            modules(
                "java.instrument",
                "java.logging",
                "java.naming",
                "java.net.http",
                "jdk.crypto.ec",
                "jdk.dynalink",
                "jdk.httpserver",
                "jdk.localedata",
                "jdk.unsupported",
            )
            windows {
                iconFile.set(project.file("icons/flix.ico"))
                menuGroup = "Flix"
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "db182a06-de11-4503-8f58-d68d27313407"
            }
        }
    }
}
