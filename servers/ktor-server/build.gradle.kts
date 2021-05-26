apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  implementation(project(Libs.core))
  implementation(project(Libs.thoolModel))
  implementation(Libs.ktorServerCore)
  implementation(Libs.nettyTransportNativeKqueue)

  testImplementation(project(Libs.test))
  testImplementation(project(Libs.ktorClient))
  testImplementation(Libs.ktorTest)
  testImplementation(Libs.ktorServerNetty)
}
