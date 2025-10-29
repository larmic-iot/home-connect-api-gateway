import org.gradle.declarative.dsl.schema.FqName.Empty.packageName

plugins {
    kotlin("multiplatform") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.codingfeline.buildkonfig") version "0.15.1"
}

group = "de.larmic.starter"
version = "1.0.0"

repositories {
    mavenCentral()
    //maven("https://packages.jetbrains.team/maven/p/kt/bootstrap")
}

val hostOs = project.findProperty("targetOs") as String? ?: System.getProperty("os.name")
val osArch = project.findProperty("targetArch") as String? ?: System.getProperty("os.arch")
val isArm64 = osArch == "aarch64" || osArch == "arm64"

val nativeTarget = when {
    hostOs == "Mac OS X" && isArm64 -> "macosArm64"
    hostOs == "Mac OS X" && !isArm64 -> "macosX64"
    hostOs.startsWith("Linux") && isArm64 -> "linuxArm64"
    hostOs.startsWith("Linux") && !isArm64 -> "linuxX64"
    hostOs.startsWith("Windows") -> "mingwX64"
    else -> throw GradleException("Unsupported host OS: $hostOs ($osArch)")
}

println("Detected native target: $nativeTarget")

kotlin {
    jvmToolchain(24)

    when (nativeTarget) {
        "macosArm64" -> macosArm64("app")
        "macosX64" -> macosX64("app")
        "linuxX64" -> linuxX64("app")
        "linuxArm64" -> linuxArm64("app")
        "mingwX64" -> mingwX64("app")
        else -> throw GradleException("Unsupported target: $nativeTarget")
    }.apply {
        binaries {
            executable {
                entryPoint = "de.larmic.starter.main"
                debuggable = true
                baseName = "app"
            }
        }
    }

    sourceSets {
        val appMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core:3.3.1")
                implementation("io.ktor:ktor-server-cio:3.3.1")
                implementation("io.ktor:ktor-server-content-negotiation:3.3.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")

                // Ktor HTTP client (Native)
                implementation("io.ktor:ktor-client-core:3.3.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
                // Engine for Native targets (Curl is cross-platform and works in Docker/Linux and macOS)
                implementation("io.ktor:ktor-client-curl:3.3.1")
            }
        }

        val appTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-server-test-host:3.3.1")
            }
        }
    }
}

buildkonfig {
    packageName = "de.larmic.starter"
    // Generate constants for all targets using defaultConfigs
    defaultConfigs {
        // Read OpenAPI file and escape for Kotlin triple-quoted string and templates
        val raw = file("resources/openapi.yaml").readText()
        val escaped = raw
            //.replace("\\", "\\\\")
            //.replace("$", "\${'$'}")
            //.replace("\"\"\"", "\\\"\\\"\\\"")
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "OPENAPI_YAML",
            escaped
        )
    }
}

// Custom task to run the debug executable on local maschine
tasks.register<Exec>("runDebug") {
    group = "application"
    description = "Builds and runs the debug native executable"

    dependsOn("linkDebugExecutableApp")

    val executablePath = when {
        hostOs.startsWith("Windows") -> "build/bin/app/debugExecutable/app.exe"
        else -> "build/bin/app/debugExecutable/app.kexe"
    }

    commandLine(project.file(executablePath).absolutePath)

    doFirst {
        println("Starting native application in debug mode...")
    }
}