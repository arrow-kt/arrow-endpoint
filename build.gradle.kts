import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  kotlin("multiplatform") version Version.kotlin apply false
  id(Plugins.kotlinSerialization) version Version.kotlin apply false
  id(Plugins.ktlint) version Version.ktlint apply true
  id("io.kotest.multiplatform") version "5.0.0.5"
  id("io.arrow-kt.arrow-gradle-config-nexus") version "0.5.1"
  id("io.arrow-kt.arrow-gradle-config-publish-multiplatform") version "0.5.1"
}

subprojects {
  if (!listOf("clients", "docs", "servers", "examples").contains(name)) {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")

    kotlin {
      explicitApi()

      targets {
        jvm {
          compilations.all {
            kotlinOptions {
              jvmTarget = "1.8"
              freeCompilerArgs += listOf(
                "Xopt-in=kotlin.RequiresOptIn"
              )
            }
          }
        }
      }

      sourceSets {
        val commonMain by getting {
          dependencies {
            implementation(Libs.kotlinStdlib)
            compileOnly(Libs.arrowCore)
          }
        }

        val jvmMain by getting {
          dependsOn(commonMain)
        }

        val commonTest by getting {
          dependsOn(commonMain)
          dependencies {
            implementation(Libs.kotlinxCoroutines)
            implementation(Libs.kotestAssertions)
            implementation(Libs.kotestProperty)
            implementation(Libs.arrowCore)
          }
        }

        val jvmTest by getting {
          dependsOn(commonTest)
          dependsOn(jvmMain)
          dependencies {
            implementation(Libs.kotestRunner)
          }
        }
      }
    }

    tasks.named<Test>("jvmTest") {
      useJUnitPlatform()
      testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
          TestLogEvent.FAILED,
          TestLogEvent.PASSED
        )
        exceptionFormat = TestExceptionFormat.FULL
      }
    }
  }
}

allprojects {
  apply(plugin = "io.kotest.multiplatform")
  apply(plugin = Plugins.ktlint)
  apply(plugin = "org.gradle.idea")

  group = "io.arrow-kt"
  version = "0.1.0-SNAPSHOT"

  repositories {
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs = listOf("-Xjsr305=strict")
    }
  }

  ktlint {
    filter {
      exclude("build.gradle.kts") // TODO: fix doesnt inspect kts file with correct indent correctly
    }
  }
}

fun Project.kotlin(configure: Action<KotlinMultiplatformExtension>): Unit =
  (this as ExtensionAware).extensions.configure("kotlin", configure)

fun KotlinMultiplatformExtension.targets(configure: Action<Any>): Unit =
  (this as ExtensionAware).extensions.configure("targets", configure)

fun KotlinMultiplatformExtension.sourceSets(configure: Action<NamedDomainObjectContainer<KotlinSourceSet>>): Unit =
  (this as ExtensionAware).extensions.configure("sourceSets", configure)
