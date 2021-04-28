plugins {
    id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.htt4kClient))
  implementation(project(Libs.springClientWeb))
  implementation(project(Libs.springClientWebFlux))

  implementation(project(":docs:openapi-docs"))

  implementation(Libs.kotlinxSerializationJson)
  implementation(Libs.http4kApache)
  api(Libs.kotestRunner)
  api(Libs.kotestAssertions)
  api(Libs.kotestProperty)
  implementation(Libs.mockwebserver)
}
