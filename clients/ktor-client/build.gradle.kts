@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

dependencies {
  api(projects.arrowEndpointCore)
  api(libs.ktor.client.core)

  testImplementation(projects.arrowEndpointCore)
  testImplementation(libs.ktor.client.cio)
  testImplementation(projects.arrowEndpointTest)
  testImplementation(libs.coroutines.core)
  testImplementation(libs.kotest.assertionsCore)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.runnerJUnit5)
}
