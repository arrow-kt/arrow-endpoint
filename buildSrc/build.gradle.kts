plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
  }
  gradlePluginPortal()
}
