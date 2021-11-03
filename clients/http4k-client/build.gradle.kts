kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(projects.core)
        implementation(libs.http4k.core)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.core)
        implementation(projects.test)
        implementation(libs.http4k.client.apache)
      }
    }
  }
}
