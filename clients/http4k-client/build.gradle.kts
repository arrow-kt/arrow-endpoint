apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.http4kCore)

  testImplementation(project(Libs.test))
  testImplementation(Libs.http4kApache)
}
