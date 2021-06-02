apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  compileOnly(project(Libs.core))
  implementation(Libs.ktorServerCore)
  implementation(Libs.nettyTransportNativeKqueue)

  testImplementation(project(Libs.core))
  testImplementation(project(Libs.test))
  testImplementation(project(Libs.ktorClient))
  testImplementation(Libs.ktorTest)
  testImplementation(Libs.ktorServerNetty)
}
