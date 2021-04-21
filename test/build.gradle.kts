plugins {
    id(Plugins.kotlinSerialization)
}

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.ktor))
  implementation(project(Libs.htt4kClient))
  implementation(Libs.kotlinxSerializationJson)
  implementation(Libs.http4kApache)
  implementation(Libs.ktorTest)
  implementation(Libs.ktorServerNetty)
  api(Libs.kotestRunner)
  api(Libs.kotestAssertions)
  api(Libs.kotestProperty)
  implementation("com.squareup.okhttp3:mockwebserver:4.9.1")
}
