apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  compileOnly(project(Libs.thoolModel))
  compileOnly(project(Libs.core))
  implementation(Libs.springBootStarterWeb)
  testImplementation(project(Libs.thoolModel))
  testImplementation(project(Libs.core))
  testImplementation(project(Libs.test))
}
