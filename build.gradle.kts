import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.kotest.multiplatform)
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.publishMultiplatform)
}

kotlin {
  jvm()
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.freeCompilerArgs += "Xopt-in=kotlin.RequiresOptIn"
}

subprojects {
  if (!listOf("clients", "docs", "servers", "examples").contains(name)) {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")

    kotlin {
      explicitApi()
      jvm()

      targets {
        jvm {
          compilations.all {
            kotlinOptions {
              jvmTarget = "1.8"
            }
          }
        }
      }

      sourceSets {
        val commonMain by getting {
          dependencies {
            implementation(rootProject.libs.kotlin.stdlibCommon)
            implementation(rootProject.libs.arrow.core)
          }
        }

        val jvmMain by getting {
          dependsOn(commonMain)
        }

        val commonTest by getting {
          dependsOn(commonMain)
          dependencies {
            implementation(rootProject.libs.coroutines.core)
            implementation(rootProject.libs.kotest.assertionsCore)
            implementation(rootProject.libs.kotest.property)
          }
        }

        val jvmTest by getting {
          dependsOn(commonTest)
          dependsOn(jvmMain)
          dependencies {
            implementation(rootProject.libs.kotest.runnerJUnit5)
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
}

fun Project.kotlin(configure: Action<KotlinMultiplatformExtension>): Unit =
  (this as ExtensionAware).extensions.configure("kotlin", configure)

fun KotlinMultiplatformExtension.targets(configure: Action<Any>): Unit =
  (this as ExtensionAware).extensions.configure("targets", configure)

fun KotlinMultiplatformExtension.sourceSets(configure: Action<NamedDomainObjectContainer<KotlinSourceSet>>): Unit =
  (this as ExtensionAware).extensions.configure("sourceSets", configure)
