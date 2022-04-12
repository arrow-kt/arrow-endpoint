import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  base
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotest.multiplatform) apply false
  alias(libs.plugins.kotlinxSerialization) apply false
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.kotlin) apply false
  alias(libs.plugins.arrowGradleConfig.versioning)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.kotlin.binaryCompatibilityValidator)
}

allprojects {
  apply(plugin = "io.kotest.multiplatform")
  apply(plugin = "org.gradle.idea")


  repositories {
    mavenCentral()
  }
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
      freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-runtime-version-check")
      jvmTarget = "1.8"
    }
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
  }
}
