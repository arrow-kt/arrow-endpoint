kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        implementation(project(Libs.core))
        runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}")
      }
    }
  }
}
