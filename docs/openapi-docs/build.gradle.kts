plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.thoolModel))
  implementation(Libs.kotlinxSerializationJson)
}
