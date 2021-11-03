kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        api(projects.core)
        api(libs.ktor.client.core)
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
