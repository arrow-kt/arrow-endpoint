dependencies {
  compileOnly(project(Libs.core))
  implementation(Libs.ktorClientCore)

  testImplementation(project(Libs.core))
  testImplementation(Libs.ktorClientCio)
  testImplementation(project(Libs.test))
}
