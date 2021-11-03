kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(projects.core)
        runtimeOnly(libs.kotlin.reflect)
      }
    }
  }
}
