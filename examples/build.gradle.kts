plugins {
  kotlin("jvm")
  application
}

apply(plugin = "kotlinx-serialization")

dependencies {
  implementation(projects.core)
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
