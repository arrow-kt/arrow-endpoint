kotlin {
  sourceSets {
    jvmMain {
      dependencies {
        compileOnly(project(Libs.core))
        implementation(Libs.ktorServerCore)
        implementation(Libs.nettyTransportNativeKqueue)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Libs.core))
        implementation(project(Libs.test))
        implementation(project(Libs.ktorClient))
        implementation(Libs.ktorTest)
        implementation(Libs.ktorServerNetty)
      }
    }
  }
}
