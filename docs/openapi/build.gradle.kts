plugins {
  alias(libs.plugins.kotlinxSerialization)
}

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
