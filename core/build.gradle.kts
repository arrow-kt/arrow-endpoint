kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Needed for Uri MatchNamedGroupCollection, ties us to JDK8
        // TODO https://app.clickup.com/t/kt7qd2
        api(libs.kotlin.stdlibJDK8)
        api(libs.coroutines.core)
      }
    }
  }
}
