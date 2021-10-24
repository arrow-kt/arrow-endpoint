plugins {
  id(Plugins.kotlinSerialization)
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.ktorServer))
  implementation(project(Libs.openApiDocs))
  implementation(Libs.kotlinStdlib)
  implementation(Libs.arrowCore)
  implementation(Libs.ktorServerCore)
  implementation(Libs.ktorServerNetty)
  implementation(Libs.logbackClassic)
  implementation(Libs.kotlinxSerializationJson)
}

application {
  mainClass.set("TestKt")
}
