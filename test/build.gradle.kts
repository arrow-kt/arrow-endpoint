plugins {
  id(Plugins.kotlinSerialization)
}

kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        implementation(projects.clients.http4k)
        implementation(projects.clients.springWeb)
        implementation(projects.clients.springWebFlux)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.http4k.client.apache)
        api(libs.kotest.runnerJUnit5)
        api(libs.kotest.assertionsCore)
        api(libs.kotest.property)
        implementation(libs.mockwebserver)
      }
    }
  }
}
