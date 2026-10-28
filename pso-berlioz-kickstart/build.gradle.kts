description = "Simple jar for Servlet 3.0 to make it easier to configure a Berlioz site"
extra["title"] = "Berlioz Kickstart"

dependencies {
  api(project(":pso-berlioz"))
  compileOnly(libs.servlet.api)

  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly(libs.saxon.he)
}
