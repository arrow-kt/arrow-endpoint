plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.javaGraphQL)

  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)
  testImplementation(Libs.kotlinxSerializationJson)
}
