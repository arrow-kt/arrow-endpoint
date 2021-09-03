import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl` apply true
  kotlin("jvm") version Version.kotlin apply false
  id(Plugins.kotlinSerialization) version Version.kotlin apply false
  id(Plugins.ktlint) version Version.ktlint apply true
}

allprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = Plugins.ktlint)
  apply(plugin = "org.gradle.idea")

  group = "com.fortysevendegrees"
  version = "0.0.1-SNAPSHOT"

  repositories {
    google()
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/arrow-kt/arrow-kt/")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }

  // Common dependencies
  dependencies {
    implementation(Libs.kotlinStdlib)
    implementation(Libs.arrowCore)
    testImplementation(Libs.kotestRunner)
    testImplementation(Libs.kotestAssertions)
    testImplementation(Libs.kotestProperty)
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs = listOf("-Xjsr305=strict")
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }

  ktlint {
    filter {
      exclude("build.gradle.kts") // TODO: fix doesnt inspect kts file with correct indent correctly
    }
  }

  kotlin {
    explicitApi()
  }
}
