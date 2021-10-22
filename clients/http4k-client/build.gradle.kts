dependencies {
  compileOnly(project(Libs.core))
  implementation(Libs.http4kCore)

  testImplementation(project(Libs.core))
  testImplementation(project(Libs.test))
  testImplementation(Libs.http4kApache)
}
