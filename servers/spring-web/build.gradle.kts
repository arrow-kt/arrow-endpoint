kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        implementation(libs.coroutines.reactive)
        implementation(libs.coroutines.reactor)
        implementation(libs.spring.boot.starter.webflux)
        implementation(libs.reactor.kotlin.extensions)
        implementation(libs.netty.transport.native.kqueue)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.test)
        implementation(projects.clients.springWebFlux)
        implementation(libs.undertow)
      }
    }
  }
}
