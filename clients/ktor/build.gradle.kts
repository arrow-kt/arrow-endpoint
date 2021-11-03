kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        implementation(libs.ktor.client.core)
      }
    }
    jvmTest {
      dependencies {
        implementation(projects.test)
        implementation(libs.ktor.client.cio)
      }
    }
  }
}
