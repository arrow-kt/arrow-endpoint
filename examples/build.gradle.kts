@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  application
}

apply(plugin = "kotlinx-serialization")

dependencies {
  api(projects.core)
  implementation(projects.servers.ktorServer)
  implementation(projects.docs.openapiDocs)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.logback.classic)
  implementation(libs.kotlinx.serialization.json)
}

application {
  mainClass.set("TestKt")
}
