import java.nio.file.Paths

apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  // Needed for Uri MatchNamedGroupCollection, ties us to JDK8
  // TODO https://app.clickup.com/t/kt7qd2
  implementation(kotlin("stdlib-jdk8"))
  implementation(Libs.kotlinxCoroutines)
  implementation(Libs.kotlinxCoroutinesJdk8)
}

