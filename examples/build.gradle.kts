@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  application
}

apply(plugin = "kotlinx-serialization")

dependencies {
  api(projects.arrowEndpointCore)
  implementation(projects.arrowEndpointKtorServer)
  implementation(projects.arrowEndpointOpenapiDocs)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.spring.boot.starter.webflux)
  implementation(libs.logback.classic)
  implementation(libs.kotlinx.serialization.json)
  implementation(projects.arrowEndpointSpringWebfluxServer)
}

application {
  mainClass.set("TestKt")
}
