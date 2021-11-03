kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        implementation(libs.ktor.server.core)
        implementation(libs.netty.transport.native.kqueue)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.test)
        implementation(projects.clients.ktor)
        implementation(libs.ktor.test)
        implementation(libs.ktor.server.netty)
      }
    }
  }
}
