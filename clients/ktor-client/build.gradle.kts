dependencies {
  implementation(Libs.ktorClientCore)
  implementation(project(Libs.core))

  testImplementation(Libs.ktorClientCio)
  testImplementation(project(Libs.test))
}
