kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(project(Libs.core))
        implementation(Libs.ktorClientCore)
      }
    }
    jvmTest {
      dependencies {
        implementation(project(Libs.core))
        implementation(Libs.ktorClientCio)
        implementation(project(Libs.test))
      }
    }
  }
}
