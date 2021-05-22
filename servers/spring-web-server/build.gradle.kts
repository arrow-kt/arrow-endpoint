dependencies {
  implementation(project(Libs.core))
  implementation(Libs.kotlinxCoroutinesReactive)
  implementation(Libs.kotlinxCoroutinesReactor)
  implementation(Libs.springBootStarterWebflux)
  implementation(Libs.reactorKotlinExtensions)
  implementation(Libs.nettyTransportNativeKqueue)

  testImplementation(project(Libs.test))
  testImplementation(project(Libs.springClientWebFlux))
  testImplementation(Libs.undertow)
}
