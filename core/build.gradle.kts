plugins {
  id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.arrowCore)
  implementation(Libs.kotlinxCoroutinesJdk8)
  implementation(project(Libs.thoolModel))
  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)

  testImplementation(Libs.kotlinxSerializationJson)
  testImplementation(project(Libs.ktorServer))
  testImplementation(Libs.ktorTest)
  testImplementation(Libs.ktorServerNetty)
  testImplementation("org.http4k:http4k-client-apache:4.7.0.2")
}
