@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
}

apply(plugin = "kotlinx-serialization")

dependencies {
  api(projects.core)
  api(libs.kotest.frameworkEngine)
  api(libs.kotest.assertionsCore)
  api(libs.kotest.property)

  implementation(projects.clients.http4kClient)
  implementation(projects.clients.springWebClient)
  implementation(projects.clients.springWebFluxClient)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.http4k.client.apache)
  implementation(libs.mockwebserver)
}
