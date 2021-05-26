apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  implementation(Libs.ktorClientCore)
  implementation(project(Libs.core))

  testImplementation(Libs.ktorClientCio)
  testImplementation(project(Libs.test))
}
