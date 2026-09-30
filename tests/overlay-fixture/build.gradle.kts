plugins { `java-library` }
description = "Non-published packaged overlay integration fixture"
repositories { mavenCentral() }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }
dependencies {
  implementation(project(":pso-berlioz"))
  compileOnly(libs.servlet.api)
}
