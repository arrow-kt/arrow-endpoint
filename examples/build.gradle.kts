
plugins {
  id(Plugins.kotlinSerialization)
  application
}

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.ktorServer))
  implementation(project(Libs.openApiDocs))
  implementation(Libs.ktorServerCore)
  implementation(Libs.ktorServerNetty)
  implementation(Libs.logbackClassic)
  implementation(Libs.kotlinxSerializationJson)
}

application {
  mainClass.set("TestKt")
}
