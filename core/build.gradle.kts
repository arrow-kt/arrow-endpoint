import java.nio.file.Paths

apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  compileOnly(project(Libs.thoolModel))
  implementation(Libs.kotlinxCoroutines)
  testImplementation(project(Libs.thoolModel))
}

tasks.withType<Jar>() {
  from(
      zipTree(sourceSets["main"].compileClasspath.find {
        it.absolutePath.contains(Paths.get("thool-model").toString())
      })
  )
}
