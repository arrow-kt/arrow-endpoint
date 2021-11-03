kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        implementation(libs.http4k.core)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.test)
        implementation(libs.http4k.client.apache)
      }
    }
  }
}
