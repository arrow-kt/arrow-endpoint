kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(projects.core)
        implementation(libs.spring.boot.starter.web)
        implementation(libs.coroutines.core)
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
