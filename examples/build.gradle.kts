import org.jetbrains.kotlin.ir.backend.js.compile

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
  implementation(libs.spring.boot.starter.webflux)
  implementation(libs.logback.classic)
  implementation(libs.kotlinx.serialization.json)
  implementation(project(":servers:spring-web-server"))
}

application {
  mainClass.set("TestKt")
}
