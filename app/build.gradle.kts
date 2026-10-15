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
    implementation(libs.datastore.preferences.core)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    systemProperty("flix.liveTests", providers.gradleProperty("liveTests").getOrElse("false"))
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
            packageVersion = "1.0.0"
            description = "Flix Music para escritorio"
            vendor = "Jhon David Toro"
        }
    }
}
