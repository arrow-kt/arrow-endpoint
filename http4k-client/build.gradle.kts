dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.thoolModel))
  implementation("org.http4k:http4k-core:4.7.0.2")

  // example
  implementation("org.http4k:http4k-client-apache:4.7.0.2")
}
