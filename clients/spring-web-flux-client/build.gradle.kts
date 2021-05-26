apply(from = "https://raw.githubusercontent.com/arrow-kt/arrow/main/arrow-libs/gradle/publication.gradle")

dependencies {
  implementation(project(Libs.core))
  implementation(Libs.kotlinxCoroutinesReactive)
  implementation(Libs.kotlinxCoroutinesReactor)
  implementation(Libs.springBootStarterWebflux)
  implementation(Libs.nettyTransportNativeKqueue)
  testImplementation(project(Libs.test))
}
