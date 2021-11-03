plugins {
  id(Plugins.kotlinSerialization)
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
