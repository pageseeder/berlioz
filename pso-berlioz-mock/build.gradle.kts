description = "A collection of mock classes for testing Berlioz application"
extra["title"] = "Berlioz Mock"

dependencies {
  api(project(":pso-berlioz"))
  compileOnly(libs.servlet.api)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.servlet.api)

  testRuntimeOnly(libs.junit.platform.launcher)
}
