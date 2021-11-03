kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Needed for Uri MatchNamedGroupCollection, ties us to JDK8
        // TODO https://app.clickup.com/t/kt7qd2
        api(libs.arrow.core)
        implementation(libs.kotlin.stdlibCommon)
        implementation(libs.coroutines.core)
        api(libs.ktor.io)
      }
    }
  }
}
