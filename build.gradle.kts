import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

// set these to com.github.[github-user-name], as such we can publish the library
// both locally and through jitpack

group = "com.github.edwinRNDR"
version = "master-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId =  project.group as String //"com.github.edwinRNDR"
            artifactId = project.name
            version = project.version as String//"master-SNAPSHOT"

            from(components["java"])
        }
    }
}
val applicationMainClass = "TemplateProgramKt"

/**  ## additional ORX features to be added to this project */
val orxFeatures = setOf(
//  "orx-boofcv",
//  "orx-camera",
//  "orx-chataigne",
    "orx-color",
    "orx-compositor",
//  "orx-dnk3",
//  "orx-easing",
//  "orx-file-watcher",
//  "orx-filter-extension",
    "orx-fx",
//  "orx-glslify",
//  "orx-gradient-descent",
    "orx-git-archiver",
    "orx-gui",
    "orx-image-fit",
//  "orx-integral-image",
//  "orx-interval-tree",
//  "orx-jumpflood",
//  "orx-kdtree",
//  "orx-keyframer",      
//  "orx-kinect-v1",
//  "orx-kotlin-parser",
//  "orx-mesh-generators",
//  "orx-midi",
//  "orx-minim",
//  "orx-no-clear",
    "orx-noise",
//  "orx-obj-loader",
    "orx-olive",
//  "orx-osc",
//  "orx-palette",
    "orx-panel",
//  "orx-parameters",
//  "orx-poisson-fill",
//  "orx-rabbit-control",
//  "orx-realsense2",
//  "orx-runway",
    "orx-shade-styles",
//  "orx-shader-phrases",
    "orx-shapes",
//  "orx-syphon",
//  "orx-temporal-blur",
//  "orx-tensorflow",    
//  "orx-time-operators",
//  "orx-timer",
//  "orx-triangulation",
//  "orx-video-profiles",
    null
).filterNotNull()

/** ## additional ORML features to be added to this project */
val ormlFeatures = setOf<String>(
//    "orml-blazepose",
//    "orml-dbface",
//    "orml-facemesh",
//    "orml-image-classifier",
//    "orml-psenet",
//    "orml-ssd",
//    "orml-style-transfer",
//    "orml-super-resolution",
//    "orml-u2net"
)

/** ## additional OPENRNDR features to be added to this project */
val openrndrFeatures = setOfNotNull(
    if (DefaultNativePlatform("current").architecture.name != "arm-v8") "video" else null
)

/** ## configure the type of logging this project uses */
enum class Logging { NONE, SIMPLE, FULL }

val applicationLogging = Logging.FULL

// ------------------------------------------------------------------------------------------------------------------ //

plugins {
    java
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
//    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
    alias(libs.plugins.gitarchive.tomarkdown).apply(false)
}

repositories {
    mavenCentral()
}

dependencies {

//    implementation(libs.jsoup)
//    implementation(libs.gson)
//    implementation(libs.csv)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)

    when (applicationLogging) {
        Logging.NONE -> {
            runtimeOnly(libs.slf4j.nop)
        }
        Logging.SIMPLE -> {
            runtimeOnly(libs.slf4j.simple)
        }
        Logging.FULL -> {
            runtimeOnly(libs.log4j.slf4j)
            runtimeOnly(libs.jackson.databind)
            runtimeOnly(libs.jackson.json)
        }
    }
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.junit)
}

// ------------------------------------------------------------------------------------------------------------------ //

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

// ------------------------------------------------------------------------------------------------------------------ //

class Openrndr {
    val openrndrVersion = libs.versions.openrndr.get()
    val orxVersion = libs.versions.orx.get()
    val ormlVersion = libs.versions.orml.get()

    // choices are "orx-tensorflow-gpu", "orx-tensorflow"
    val orxTensorflowBackend = "orx-tensorflow"

    val os = if (project.hasProperty("targetPlatform")) {
        val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")
        val platform: String = project.property("targetPlatform") as String
        if (platform !in supportedPlatforms) {
            throw IllegalArgumentException("target platform not supported: $platform")
        } else {
            platform
        }
    } else when (OperatingSystem.current()) {
        OperatingSystem.WINDOWS -> "windows"
        OperatingSystem.MAC_OS -> when (val h = DefaultNativePlatform("current").architecture.name) {
            "arm-v8" -> "macos-arm64"
            else -> "macos"
        }
        OperatingSystem.LINUX -> when (val h = DefaultNativePlatform("current").architecture.name) {
            "x86-64" -> "linux-x64"
            "aarch64" -> "linux-arm64"
            else -> throw IllegalArgumentException("architecture not supported: $h")
        }
        else -> throw IllegalArgumentException("os not supported")
    }

    fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
    fun orml(module: String) = "org.openrndr.orml:$module:$ormlVersion"
    fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndrVersion"
    fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"
    fun orxNatives(module: String) = "org.openrndr.extra:$module-natives-$os:$orxVersion"

    init {
        repositories {
            listOf(openrndrVersion, orxVersion, ormlVersion).any { it.contains("SNAPSHOT") }.ifTrue { mavenLocal() }
            maven(url = "https://maven.openrndr.org")
        }
        dependencies {
            runtimeOnly(openrndr("gl3"))
            runtimeOnly(openrndrNatives("gl3"))
            implementation(openrndr("openal"))
            runtimeOnly(openrndrNatives("openal"))
            implementation(openrndr("application"))
            implementation(openrndr("svg"))
            implementation(openrndr("animatable"))
            implementation(openrndr("extensions"))
            implementation(openrndr("filter"))
            if ("video" in openrndrFeatures) {
                implementation(openrndr("ffmpeg"))
                runtimeOnly(openrndrNatives("ffmpeg"))
            }
            for (feature in orxFeatures) {
                implementation(orx(feature))
            }
            for (feature in ormlFeatures) {
                implementation(orml(feature))
            }
            if ("orx-tensorflow" in orxFeatures) runtimeOnly("org.openrndr.extra:$orxTensorflowBackend-natives-$os:$orxVersion")
            if ("orx-kinect-v1" in orxFeatures) runtimeOnly(orxNatives("orx-kinect-v1"))
            if ("orx-olive" in orxFeatures) implementation(libs.kotlin.script.runtime)
        }
    }
}
val openrndr = Openrndr()
