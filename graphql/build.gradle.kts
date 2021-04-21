plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.javaGraphQL)

  testImplementation(Libs.kotlinxSerializationJson)
}
