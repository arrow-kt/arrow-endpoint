kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(project(Libs.core))
        implementation(Libs.springBootStarterWeb)
        implementation(Libs.kotlinxCoroutines)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Libs.core))
        implementation(project(Libs.test))
      }
    }
  }
}
