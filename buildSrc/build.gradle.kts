plugins {
  `kotlin-dsl`
  kotlin("jvm") version "1.4.32"
}

repositories {
  jcenter()
  mavenCentral()
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
