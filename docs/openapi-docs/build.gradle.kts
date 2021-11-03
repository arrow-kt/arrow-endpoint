plugins {
  id(Plugins.kotlinSerialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(project(Libs.core))
        implementation(Libs.kotlinxSerializationJson)
      }
    }
  }
}
