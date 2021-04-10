dependencies {
  implementation(project(Libs.core))
  implementation("org.jetbrains.kotlin:kotlin-stdlib")
  runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
  testImplementation(Libs.kotestRunner)
  testImplementation(Libs.kotestAssertions)
  testImplementation(Libs.kotestProperty)
}
