plugins {
  id(Plugins.kotlinSerialization)
}
dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.thoolModel))
  implementation(Libs.ktorServerCore)
  implementation(Libs.ktorServerNetty)
  implementation(Libs.logbackClassic)
  implementation(Libs.kotlinxSerializationJson)
}
