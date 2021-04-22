dependencies {
  api(project(Libs.thoolModel))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.kotlinxCoroutinesJdk8)

  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)
}
