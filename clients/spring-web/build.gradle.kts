kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        implementation(libs.spring.boot.starter.web)
        implementation(libs.coroutines.core)
      }
    }

    jvmTest {
      dependencies {
        implementation(projects.test)
      }
    }
  }
}
