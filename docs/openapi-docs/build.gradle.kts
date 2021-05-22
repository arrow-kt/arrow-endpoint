plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.kotlinxSerializationJson)
}
