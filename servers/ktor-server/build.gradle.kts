kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(projects.core)
        implementation(libs.ktor.server.core)
        implementation(libs.netty.transport.native.kqueue)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.core)
        implementation(projects.test)
        implementation(projects.clients.ktorClient)
        implementation(libs.ktor.test)
        implementation(libs.ktor.server.netty)
      }
    }
  }
}
