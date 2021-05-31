plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  compileOnly(project(Libs.thoolModel))
  compileOnly(project(Libs.core))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.javaGraphQL)

  testImplementation(project(Libs.thoolModel))
  testImplementation(project(Libs.core))
  testImplementation(Libs.kotlinxSerializationJson)
}
