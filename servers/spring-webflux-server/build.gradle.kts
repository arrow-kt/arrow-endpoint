@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

dependencies {
  api(projects.arrowEndpointCore)
  implementation(libs.coroutines.reactive)
  implementation(libs.coroutines.reactor)
  implementation(libs.spring.boot.starter.webflux)
  implementation(libs.reactor.kotlin.extensions)
  implementation(libs.netty.transport.native.kqueue)

  testImplementation(projects.arrowEndpointTest)
//  testImplementation(projects.arrowEndpointSpringWebFluxClient)
  testImplementation(libs.undertow)
  testImplementation(rootProject.libs.coroutines.core)
  testImplementation(rootProject.libs.kotest.assertionsCore)
  testImplementation(rootProject.libs.kotest.property)
  testImplementation(rootProject.libs.kotest.runnerJUnit5)
}
