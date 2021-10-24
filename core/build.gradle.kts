kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(kotlin("stdlib", Version.kotlin))
        implementation(Libs.kotlinxCoroutines)
        implementation(Libs.ktorio)
      }
    }

    jvmMain {
      dependencies {
        // Needed for Uri MatchNamedGroupCollection, ties us to JDK8
        // TODO https://app.clickup.com/t/kt7qd2
        implementation(kotlin("stdlib-jdk8", Version.kotlin))
      }
    }
  }
}
