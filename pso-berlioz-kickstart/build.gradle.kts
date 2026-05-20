description = "Simple jar for Servlet 3.0 to make it easier to configure a Berlioz site"
extra["title"] = "Berlioz Kickstart"

dependencies {
  api(project(":"))
  compileOnly(libs.servlet.api)
}

tasks.withType<Javadoc>().configureEach {
  isFailOnError = false
}
