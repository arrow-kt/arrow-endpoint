apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.springBootStarterWeb)
  testImplementation(project(Libs.test))
}
