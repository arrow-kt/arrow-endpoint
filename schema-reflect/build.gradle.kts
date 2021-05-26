apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  implementation(project(Libs.core))
  runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
}
