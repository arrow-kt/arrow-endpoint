dependencies {
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.kotlinxCoroutinesJdk8)
  api(project(Libs.thoolModel))

  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)
}
