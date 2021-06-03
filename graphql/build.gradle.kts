plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  compileOnly(project(Libs.core))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.javaGraphQL)

  testImplementation(project(Libs.core))
  testImplementation(Libs.kotlinxSerializationJson)
}
