kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(project(Libs.core))
        implementation(Libs.http4kCore)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Libs.core))
        implementation(project(Libs.test))
        implementation(Libs.http4kApache)
      }
    }
  }
}
