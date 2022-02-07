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
        api(libs.kotlin.stdlibCommon)
        api(libs.arrow.core)
        api(libs.coroutines.core)
        api(libs.ktor.io)
        // api(libs.ktor.http)
      }
    }

    jsMain {
      dependencies {
        api(npm("punycode", "2.1.1"))
        api(npm("buffer", "6.0.3"))
        api(npm("string_decoder", "1.3.0"))
      }
    }

    nativeMain {
      dependencies {

      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
        implementation(libs.kotest.property)
      }
    }

    jvmTest {
      dependencies {
        implementation(rootProject.libs.kotest.runnerJUnit5)
      }
    }
  }
}
