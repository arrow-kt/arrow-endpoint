dependencies {
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.arrowCore)
  implementation(Libs.kotlinxCoroutinesJdk8)
  implementation(project(Libs.thoolModel))
  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)
}
