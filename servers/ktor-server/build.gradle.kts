dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.thoolModel))
  implementation(Libs.ktorServerCore)

  testImplementation(project(Libs.test))
  testImplementation(Libs.ktorServerNetty)
}
