@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
}

dependencies {
  api(projects.core)
  api(libs.ktor.server.core)

  testImplementation(projects.test)
  testImplementation(projects.clients.ktorClient)
  testImplementation(libs.ktor.test)
  testImplementation(libs.ktor.server.netty)
  testImplementation(libs.coroutines.core)
  testImplementation(libs.kotest.assertionsCore)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.runnerJUnit5)
}
