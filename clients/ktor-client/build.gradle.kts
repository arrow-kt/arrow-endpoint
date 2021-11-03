kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(projects.core)
        implementation(libs.ktor.client.core)
      }
    }
    jvmTest {
      dependencies {
        implementation(projects.core)
        implementation(libs.ktor.client.cio)
        implementation(projects.test)
      }
    }
  }
}
