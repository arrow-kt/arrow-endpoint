object Version {
  const val kotlin: String = "1.5.10"
  const val arrow: String = "0.13.2"
  const val kotlinx: String = "1.4.3"
  const val kotlinxSerializationJson: String = "1.2.1"
  const val ktlint: String = "10.0.0"
  const val ktor: String = "1.5.4"
  const val logbackClassic: String = "1.2.3"
  const val spring: String = "2.4.5"
  const val reactorKotlinExtensions: String = "1.1.3"
  const val undertow: String = "2.2.7.Final"
  const val kotest: String = "4.4.3"
  const val graphQL: String = "16.2"
  const val kotlinGraphQL: String = "4.0.0-alpha.17"
  const val http4k: String = "4.7.0.2"
  const val nettyTransport = "4.1.63.Final"
  const val http3: String = "4.9.1"
}

object Libs {
  const val core: String = ":core"
  const val graphQL: String = ":graphql"
  const val examples: String = ":examples"
  const val ktorServer: String = ":servers:ktor-server"
  const val ktorClient: String = ":clients:ktor-client"
  const val htt4kClient: String = ":clients:http4k-client"
  const val springClientWeb: String = ":clients:spring-web-client"
  const val springClientWebFlux: String = ":clients:spring-web-flux-client"
  const val openApiDocs: String = ":docs:openapi-docs"
  const val test: String = ":test"
  const val kotlinStdlib: String = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"

  const val kotlinxCoroutines: String = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinx}"
  const val kotlinxCoroutinesReactive: String = "org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Version.kotlinx}"
  const val kotlinxCoroutinesReactor: String = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Version.kotlinx}"
  const val kotlinxSerializationJson: String =
    "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinxSerializationJson}"
  const val kotlinxCoroutinesJdk8: String = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Version.kotlinx}"

  const val arrowCore: String = "io.arrow-kt:arrow-core:${Version.arrow}"

  const val javaGraphQL: String = "com.graphql-java:graphql-java:${Version.graphQL}"
  const val graphQlServer = "com.expediagroup:graphql-kotlin-server:${Version.kotlinGraphQL}"

  const val http4kCore = "org.http4k:http4k-core:${Version.http4k}"
  const val http4kApache = "org.http4k:http4k-client-apache:${Version.http4k}"

  const val mockwebserver: String = "com.squareup.okhttp3:mockwebserver:${Version.http3}"

  const val ktorServerCore: String = "io.ktor:ktor-server-core:${Version.ktor}"
  const val ktorServerNetty: String = "io.ktor:ktor-server-netty:${Version.ktor}"
  const val ktorClientCore: String = "io.ktor:ktor-client-core:${Version.ktor}"
  const val ktorClientCio: String = "io.ktor:ktor-client-cio:${Version.ktor}"
  const val ktorTest: String = "io.ktor:ktor-server-test-host:${Version.ktor}"
  const val logbackClassic: String = "ch.qos.logback:logback-classic:${Version.logbackClassic}"

  const val springBootStarterWeb = "org.springframework.boot:spring-boot-starter-web:${Version.spring}"
  const val springBootStarterWebflux = "org.springframework.boot:spring-boot-starter-webflux:${Version.spring}"
  const val reactorKotlinExtensions = "io.projectreactor.kotlin:reactor-kotlin-extensions:${Version.reactorKotlinExtensions}"
  const val undertow = "io.undertow:undertow-core:${Version.undertow}"
  const val nettyTransportNativeKqueue = "io.netty:netty-transport-native-kqueue:${Version.nettyTransport}"

  const val kotestRunner: String = "io.kotest:kotest-runner-junit5:${Version.kotest}"
  const val kotestAssertions: String = "io.kotest:kotest-assertions-core:${Version.kotest}"
  const val kotestProperty: String = "io.kotest:kotest-property:${Version.kotest}"
}

object Plugins {
  const val kotlinSerialization: String = "org.jetbrains.kotlin.plugin.serialization"
  const val ktlint: String = "org.jlleitschuh.gradle.ktlint"
}
