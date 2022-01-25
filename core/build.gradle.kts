@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
}

dependencies {
  api(libs.kotlin.stdlibCommon)
  api(libs.arrow.core)
  api(libs.coroutines.core)

  testImplementation(rootProject.libs.coroutines.core)
  testImplementation(rootProject.libs.kotest.assertionsCore)
  testImplementation(rootProject.libs.kotest.property)
  testImplementation(rootProject.libs.kotest.runnerJUnit5)
}
