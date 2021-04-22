plugins {
    id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.kotlinxSerializationJson)
  implementation(Libs.http4kApache)
  api(Libs.kotestRunner)
  api(Libs.kotestAssertions)
  api(Libs.kotestProperty)
}
