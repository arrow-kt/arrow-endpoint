dependencies {
  api(project(Libs.thoolModel))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.arrowCore)
  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)
}
