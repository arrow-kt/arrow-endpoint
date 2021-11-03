kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.arrow.core)
        implementation(libs.kotlin.stdlibCommon)
        implementation(libs.coroutines.core)
        api(libs.ktor.io)
      }
    }
  }
}
