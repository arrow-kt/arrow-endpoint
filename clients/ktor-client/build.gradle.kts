dependencies {
  implementation(Libs.ktorClientCore)
  implementation(project(Libs.core))
  testImplementation(Libs.ktorClientCio)
}
