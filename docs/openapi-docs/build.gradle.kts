apply(plugin = "kotlinx-serialization")

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
