apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  api(project(Libs.thoolModel))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.kotlinxCoroutinesJdk8)
}
