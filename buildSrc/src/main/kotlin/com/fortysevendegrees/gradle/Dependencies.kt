object Version {
  const val kotlin: String = "1.4.32"
  const val arrow: String = "0.13.1"
  const val kotlinx: String = "1.4.3"
  const val kotlinxSerializationJson: String = "1.1.0"
  const val ktlint: String = "10.0.0"
  const val ktor: String = "1.5.3"
  const val logbackClassic: String = "1.2.3"
  const val kotest: String = "4.4.3"
  const val graphQL: String = "16.2"
  const val kotlinGraphQL: String = "4.0.0-alpha.17"
  const val http4kApache = "4.7.0.2"
}

object Libs {
  const val core: String = ":core"
  const val thoolModel: String = ":thool-model"
  const val graphQL: String = ":graphql"
  const val examples: String = ":examples"
  const val ktor: String = ":ktor-server"
  const val kotlinStdlib: String = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"

  const val kotlinxCoroutines: String = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinx}"
  const val kotlinxSerializationJson: String =
    "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinxSerializationJson}"
  const val kotlinxCoroutinesJdk8: String = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Version.kotlinx}"

  const val arrowCore: String = "io.arrow-kt:arrow-core:${Version.arrow}"

  const val javaGraphQL: String = "com.graphql-java:graphql-java:${Version.graphQL}"
  const val graphQlServer = "com.expediagroup:graphql-kotlin-server:${Version.kotlinGraphQL}"

  const val http4kApache = "org.http4k:http4k-client-apache:${Version.http4kApache}"

  const val ktorServerCore: String = "io.ktor:ktor-server-core:${Version.ktor}"
  const val ktorServerNetty: String = "io.ktor:ktor-server-netty:${Version.ktor}"
  const val ktorTest: String = "io.ktor:ktor-server-test-host:${Version.ktor}"
  const val logbackClassic: String = "ch.qos.logback:logback-classic:${Version.logbackClassic}"

  const val kotestRunner: String = "io.kotest:kotest-runner-junit5:${Version.kotest}"
  const val kotestAssertions: String = "io.kotest:kotest-assertions-core:${Version.kotest}"
  const val kotestProperty: String = "io.kotest:kotest-property:${Version.kotest}"
}

object Plugins {
  const val kotlinSerialization: String = "org.jetbrains.kotlin.plugin.serialization"
  const val ktlint: String = "org.jlleitschuh.gradle.ktlint"
}
