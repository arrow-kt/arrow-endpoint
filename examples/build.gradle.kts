plugins {
  id(Plugins.kotlinSerialization)
  application
}

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.graphQL))
  implementation(project(Libs.ktorServer))
  implementation(Libs.graphQlServer)
  implementation(Libs.ktorServerCore)
  implementation(Libs.ktorServerNetty)
  implementation(Libs.logbackClassic)
  implementation(Libs.kotlinxSerializationJson)
  implementation("org.openapi4j:openapi-operation-validator:1.0.7")
}

application {
  mainClass.set("TestKt")
}
