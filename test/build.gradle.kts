apply(plugin = "kotlinx-serialization")

kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(projects.core)
        implementation(projects.clients.http4kClient)
        implementation(projects.clients.springWebClient)
        implementation(projects.clients.springWebFluxClient)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.http4k.client.apache)
        api(libs.kotest.frameworkEngine)
        api(libs.kotest.assertionsCore)
        api(libs.kotest.property)
        implementation(libs.mockwebserver)
      }
    }
  }
}
