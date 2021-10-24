kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(project(Libs.core))
        implementation(Libs.kotlinxCoroutinesReactive)
        implementation(Libs.kotlinxCoroutinesReactor)
        implementation(Libs.springBootStarterWebflux)
        implementation(Libs.reactorKotlinExtensions)
        implementation(Libs.nettyTransportNativeKqueue)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Libs.core))
        implementation(project(Libs.test))
        implementation(project(Libs.springClientWebFlux))
        implementation(Libs.undertow)
      }
    }
  }
}
