import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  base
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotest.multiplatform) apply false
  alias(libs.plugins.kotlinxSerialization) apply false
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

allprojects {
  group = property("projects.group").toString()
}

tasks {
  withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    useJUnitPlatform()
    testLogging {
      setExceptionFormat("full")
      setEvents(listOf("passed", "skipped", "failed", "standardOut", "standardError"))
    }
  }
  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-runtime-version-check", "-Xopt-in=kotlin.RequiresOptIn")
      jvmTarget = "1.8"
    }
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
  }
}

allprojects {
  apply(plugin = "io.kotest.multiplatform")
  apply(plugin = "org.gradle.idea")

  group = "io.arrow-kt"
  version = "0.1.0-SNAPSHOT"

  repositories {
    mavenCentral()
  }
}
