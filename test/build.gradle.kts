@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
}

apply(plugin = "kotlinx-serialization")

dependencies {
  api(projects.arrowEndpointCore)
  api(libs.kotest.frameworkEngine)
  api(libs.kotest.assertionsCore)
  api(libs.kotest.property)

  implementation(projects.arrowEndpointHttp4kClient)
  implementation(projects.arrowEndpointSpringWebClient)
  implementation(projects.arrowEndpointSpringWebfluxClient)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.http4k.client.apache)
  implementation(libs.mockwebserver)
}
