description = "Berlioz framework"

dependencies {
  api(libs.xmlwriter)
  implementation(libs.slf4j.api)

  compileOnly(libs.servlet.api) {
    because("This is provided by the Servlet container")
  }
  compileOnly(libs.jackson.core) {
    because("Optional dependency for JSON output using Jackson")
  }
  compileOnly(libs.gson) {
    because("Optional dependency for JSON output using Google JSON library")
  }
  compileOnly(libs.javax.json.api) {
    because("Optional dependency for JSON output using JSR 374")
  }
  compileOnly(libs.jakarta.json.api) {
    because("Optional dependency for JSON output using JSR 374")
  }
  compileOnly(libs.jspecify) {
    because("Used for null safety annotations")
  }

  testImplementation(libs.junit)
  testImplementation(libs.servlet.api)
  testImplementation(libs.slf4j.simple)
  testImplementation(libs.glassfish.javax.json)
  testImplementation(libs.glassfish.jakarta.json)
  testImplementation(libs.parsson)
  testImplementation(libs.jackson.core)
  testImplementation(libs.gson)
}

publishing {
  publications {
    named<MavenPublication>("maven") {
      pom {
        withXml {
          val dependenciesNode = asNode().appendNode("dependencies")
          listOf(
            libs.jackson.core.get(),
            libs.gson.get(),
            libs.javax.json.api.get(),
            libs.jakarta.json.api.get()
          ).forEach { dep ->
            dependenciesNode.appendNode("dependency").apply {
              appendNode("groupId", dep.group)
              appendNode("artifactId", dep.name)
              appendNode("version", dep.versionConstraint.requiredVersion)
              appendNode("scope", "compile")
              appendNode("optional", "true")
            }
          }
        }
      }
    }
  }
}
