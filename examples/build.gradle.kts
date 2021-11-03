plugins {
  alias(libs.plugins.kotlinxSerialization)
  kotlin("jvm")
  application
}

dependencies {
  implementation(projects.core)
  implementation(projects.servers.ktor)
  implementation(projects.docs.openapi)
  implementation(libs.kotlin.stdlibCommon)
  implementation(libs.arrow.core)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.logback.classic)
  implementation(libs.kotlinx.serialization.json)
}

application {
  mainClass.set("TestKt")
}
