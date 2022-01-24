@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.multiplatform.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Needed for Uri MatchNamedGroupCollection, ties us to JDK8
        // TODO https://app.clickup.com/t/kt7qd2
        api(libs.kotlin.stdlibCommon)
        api(libs.arrow.core)
        api(libs.coroutines.core)
        implementation(libs.ktor.io)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
      }
    }

    jvmTest {
      dependencies {
        implementation(rootProject.libs.coroutines.core)
        implementation(rootProject.libs.kotest.assertionsCore)
        implementation(rootProject.libs.kotest.property)
        implementation(rootProject.libs.kotest.runnerJUnit5)
      }
    }
  }
}
