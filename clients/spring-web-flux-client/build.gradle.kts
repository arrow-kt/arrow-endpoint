kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(projects.core)
        implementation(libs.coroutines.reactive)
        implementation(libs.coroutines.reactor)
        implementation(libs.spring.boot.starter.webflux)
        implementation(libs.netty.transport.native.kqueue)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.core)
        implementation(projects.test)
      }
    }
  }
}
