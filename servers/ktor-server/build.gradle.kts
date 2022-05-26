@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

dependencies {
  api(projects.arrowEndpointCore)
  api(libs.ktor.server.core)

  testImplementation(projects.arrowEndpointTest)
  testImplementation(projects.arrowEndpointKtorClient)
  testImplementation(libs.ktor.test)
  testImplementation(libs.ktor.server.netty)
  testImplementation(libs.coroutines.core)
  testImplementation(libs.kotest.assertionsCore)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.runnerJUnit5)
}
