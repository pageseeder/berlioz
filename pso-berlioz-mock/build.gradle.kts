description = "A collection of mock classes for testing Berlioz application"
extra["title"] = "Berlioz Mock"

dependencies {
  api(project(":"))
  compileOnly(libs.servlet.api)
}

tasks.withType<Javadoc>().configureEach {
  isFailOnError = false
}
